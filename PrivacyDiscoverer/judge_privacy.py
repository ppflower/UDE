#!/usr/bin/python
#-- coding:utf8 --

import sys
import os
import re
import json
import random
from collections import Counter
from nltk.corpus import wordnet as wn
from nltk import word_tokenize, pos_tag
from nltk.stem import WordNetLemmatizer
from nltk.parse.corenlp import CoreNLPDependencyParser
from nltk.parse import CoreNLPParser

from . import const
from . import config


def write_content_to_file(dir_path, file_name, content):
    if not os.path.exists(dir_path):
        os.makedirs(dir_path)

    file_path = os.path.join(dir_path, file_name)
    with open(file_path, 'w') as f:
        f.write(str(content))


def read_all_lines_from_file(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        return f.read().split('\n')


def read_not_empty_lines_from_file(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        return f.read().rstrip().split('\n')

def read_content_from_file(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        return f.read()


def extract_words(long_word):
    return re.findall(r'[a-z]{2,}|[A-Z][a-z]+', long_word)


def extract_words_lower(long_word):
    wl = extract_words(long_word)
    return [i.lower() for i in wl]


def is_obfuscated_short_class(clazz):
    clazz_list = clazz.split('$')
    for clazz_item in clazz_list:
        if not bool(clazz_item):
            continue
        if not is_obfuscated_seperate_class(clazz_item):
            return False
    return True


def is_obfuscated_seperate_class(clazz):
    if is_normal_field_or_class(clazz):
        return False
    return True


def is_obfuscated_field(field):
    if is_normal_field_or_class(field):
        return False
    if is_const_field(field):
        return False
    return True


def is_special_field(field):
    return re.match(r'^this\$\d+$', field)


def is_const_field(field):
    return field.isupper() and len(re.findall(r'[A-Z]', field)) > 1


def is_normal_field_or_class(field_or_class):
    candidate_seqs = extract_words_lower(field_or_class)

    valid_length = 0
    for seq in candidate_seqs:
        if is_normal_word(seq):
            return True
        valid_length += len(seq)
    
    letter_len = len(''.join(re.findall(r'[a-zA-Z]+', field_or_class)))
    if letter_len == 0:
        return False
    if (valid_length/letter_len - 0.75) < 0:
        return False

    if len(candidate_seqs) > 1:
        return True
    elif len(candidate_seqs) == 1:
        return is_possible_normal_word(candidate_seqs[0])

    return False


def is_possible_normal_word(seq):
    assert seq.isalpha(), 'Word should be letters!'
    assert seq.islower(), 'Word should be lower case!'
    if seq.startswith('zz'):
        return False
    elif len(seq) > 1 and len(seq) < 4:
        return seq in const.SHORT_PRIVACY_WORDS
    elif len(seq) >= 4 and len(seq) < 25:
        # length should be between 4 and 25
        if len(Counter(seq.lower())) > 2:
            return True
    return False


def is_normal_word(word):
    if len(word) < 2:
        return False
    if len(word) == 2:
        return word in const.COMMON_2C_WORDS
    else:
        return bool(wn.synsets(word))


def is_unrelated_type(field_type_chain):
    field_types = field_type_chain.split('/')
    for ft in field_types:
        for ut in const.PRIVACY_UNRELATED_TYPES:
            if re.match(ut, ft):
                return True
    return False


def is_unrelated_field(field_name):
    return is_widget(field_name) or field_name.startswith('__ID_')


def is_widget(field_name):
    words = extract_words_lower(field_name)
    for word in words:
        for widget_word in const.COMMON_WIDGETS_RELATED_WORDS:
            if word == widget_word:
                return True
    return False


def get_wordnet_pos(tag):
    if tag.startswith('J'):
        return wn.ADJ
    elif tag.startswith('V'):
        return wn.VERB
    elif tag.startswith('N'):
        return wn.NOUN
    elif tag.startswith('R'):
        return wn.ADV
    else:
        return None


def to_word_sequence_lower(field_or_class):
    word_seq = ' '.join(extract_words_lower(field_or_class))
    tokens = word_tokenize(word_seq)
    tagged_sent = pos_tag(tokens)

    wnl = WordNetLemmatizer()
    lemmas_sent = []
    for tag in tagged_sent:
        wordnet_pos = get_wordnet_pos(tag[1]) or wn.NOUN
        lemmas_sent.append(wnl.lemmatize(tag[0], pos=wordnet_pos)) # Lemmatization
    return lemmas_sent


def match_privacy_in_word_sequence(field):
    words = to_word_sequence_lower(field)
    full_word = ''.join(words)
    match_res = match_privacy(full_word)
    if match_res:
        return 1, [full_word]
    res = []
    for word in words:
        match_res = match_privacy(word)
        if match_res:
            res.append(word)
    return 0, res


def match_privacy(word):
    for cate in privacy_set:
        if word in privacy_set[cate]:
            return True
    return False


def match_privacy_category(word):
    for cate in privacy_set:
        if word in privacy_set[cate]:
            return cate
    return None


def privacy_dependence_analyze(field_or_class_name, match_res):
    if match_res[0]:
        return match_privacy_category(match_res[1][0])

    dependencies = parse_dependency(field_or_class_name)
    for privacy_item in match_res[1]:
        is_privacy_field = False
        for dp in dependencies:
            if privacy_item in dp[0]:
                if is_debug:
                    print('    [dp]', privacy_item, dp)
                if dp[1]=='compound':
                    is_privacy_field = True
                elif dp[1] == 'amod' or dp[1].startswith('nmod'):
                    # nummod brings too much fp
                    is_privacy_field = True
                elif dp[1]=='dep':
                    is_privacy_field = True
                else:
                    is_privacy_field = False
                    break
            elif privacy_item in dp[2]:
                if is_debug:
                    print('    [dp]', privacy_item, dp)
                if dp[1]=='obj' and dp[0][1] == 'VBN':
                    is_privacy_field = True
                else:
                    is_privacy_field = False
                    break

        if is_privacy_field:
            return match_privacy_category(privacy_item)

    return None


def privacy_type_check(privacy_category, field_type):
    if bool(privacy_category):
        if privacy_category in type_map:
            if field_type in type_map[privacy_category]:
                return True
            elif field_type not in const.BASIC_TYPES:
                if field_type.startswith('java.'):
                    return False
                for thirdlib in thirdlibs:
                    if field_type.startswith(thirdlib):
                        return False
                return True
    return False


def parse_dependency(field_or_class_name):
    input_string = ' '.join(extract_words_lower(field_or_class_name))
    parses, = dependency_parser.raw_parse(input_string)
    res = list(parses.triples())
    return res


def is_anonymous_inner_class(short_class_name):
    return re.match(r'.+\$\d+$', short_class_name)


def is_possible_my_privacy(short_class_name):
    word_list = extract_words_lower(short_class_name)
    for word in word_list:
        if word in const.MY_PRIVACY_OR_OBJECT_RELATED_WORDS:
            return True
    return False


def is_possible_profile_entry(short_class_name):
    word_list = extract_words_lower(short_class_name)
    for word in word_list:
        if word in const.PROFILE_ENTRY_WORDS:
            return True
    return False


def judge_field(class_name, field_type_chain, field_name):
    if is_debug:
        print('  [Field processing]', class_name, field_type_chain, field_name)
    short_class_name = class_name.split('.')[-1]
    field_type = field_type_chain.split('/')[0]

    if len(field_name) > 25:
        return 0, field_name, None

    # Judge if an item is privacy by its type
    if not is_unrelated_type(field_type_chain):
        if is_obfuscated_field(field_name): # field_name is obfuscated
            if is_debug:
                print('  - Obfuscated field:', field_name)
            if not is_obfuscated_short_class(short_class_name):
                match_res = match_privacy_in_word_sequence(short_class_name)
                dp_res = privacy_dependence_analyze(short_class_name, match_res)
                if privacy_type_check(dp_res, field_type):
                    if is_debug:
                        print('  [Field res]', class_name, field_type_chain, field_name, '->', dp_res)
                    return 1, field_name, dp_res
            elif is_debug:
                print('  - Obfuscated class:', short_class_name)
        # filter obfuscation
        elif not is_const_field(field_name) and not is_special_field(field_name):
            if not is_unrelated_field(field_name): # filter unrelated field by its name, tv textView...
                match_res = match_privacy_in_word_sequence(field_name)
                dp_res = privacy_dependence_analyze(field_name, match_res)
                if privacy_type_check(dp_res, field_type):
                    if is_debug:
                        print('  [Field res]', class_name, field_type_chain, field_name, '->', dp_res)
                    return 1, field_name, dp_res


    return 0, field_name, None


def analyze_fields(class_name, field_list):
    global field_analysis_res
    if class_name in field_analysis_res:
        return field_analysis_res[class_name]
    res = []
    for field_type_chain, field_name in field_list:
        res.append(judge_field(class_name, field_type_chain, field_name))
    field_analysis_res[class_name] = res
    return res


def include_human_related_privacy_item(field_res_list):
    for field_res in field_res_list:
        if field_res[0] and field_res[2] in const.HUMAN_RELATED_CATEGORIES:
            return True
    return False


def profile_entry_judge(class_name, field_list):
    if is_debug:
        print('[Class]', class_name)

    short_class_name = class_name.split('.')[-1]
    if is_anonymous_inner_class(short_class_name):
        return 0, None
    # if is_possible_my_privacy(short_class_name):
    #     return 0, None
    if is_obfuscated_short_class(short_class_name): # If the class name is obfuscated, we can not judge if it is other users' privacy
        return 0, None

    # analyze fields
    field_res_list = analyze_fields(class_name, field_list)
    if is_possible_profile_entry(short_class_name):
        return 1, field_res_list
    elif include_human_related_privacy_item(field_res_list):
        return 1, field_res_list
    # Any other conditions?
    return 0, None


def normal_entry_judge(class_name, field_list):
    if is_debug:
        print('[Class]', class_name)

    short_class_name = class_name.split('.')[-1]
    if is_anonymous_inner_class(short_class_name):
        return 0, None
    # if is_possible_my_privacy(short_class_name):
    #     return 0, None

    # analyze fields
    field_res_list = analyze_fields(class_name, field_list)
    return 1, field_res_list


def batch_judge(app_class_info):

    res = {}
    q = []
    for class_name in app_class_info:
        res[class_name] = None

    access_record = {}
    for class_name, field_list in app_class_info.items():
        access_record[class_name] = 0

    for class_name, field_list in app_class_info.items():
        profile_res = profile_entry_judge(class_name, field_list)
        if profile_res[0]:
            res[class_name] = profile_res[1]
            q.append(class_name)
            access_record[class_name] = 1

    while q:
        class_name = q.pop(0)
        if not bool(res[class_name]):
            entry_res = normal_entry_judge(class_name, app_class_info[class_name])
            if entry_res[0]:
                res[class_name] = entry_res[1]

        for field_type, field_name in app_class_info[class_name]:
            if field_type in access_record and not bool(access_record[field_type]):
                q.append(field_type)
                access_record[field_type] = 1
    
    global field_analysis_res
    field_analysis_res = {}
    return res


def init():
    global dependency_parser
    global privacy_set
    global type_map
    global thirdlibs
    
    dependency_parser = CoreNLPDependencyParser(url=config.DEPENDENCY_PARSER_HOST)

    # LEXICON_FILE_PATH = os.path.join(os.path.dirname(__file__), "lexicon.json")
    with open(config.LEXICON_FILE_PATH, mode="r", encoding="utf-8") as f:
        privacy_set = json.load(f)

    # LEXICON_TYPE_FILE_PATH = os.path.join(os.path.dirname(__file__), "lexicon_type_map.json")
    with open(config.LEXICON_TYPE_FILE_PATH, mode="r", encoding="utf-8") as f:
        type_map = json.load(f)

    THIRDLIBS_PATH = "../ThirdLibs.txt"
    if os.path.exists(THIRDLIBS_PATH):
        thirdlibs = read_not_empty_lines_from_file(THIRDLIBS_PATH)
    else:
        print("Error, ThirdLibs.txt not found!")
    pass



dependency_parser = None

is_debug = False
# is_debug = True

privacy_set = {}
type_map = {}
thirdlibs = []

field_analysis_res = {}

init()


def test():
    input_data = {
        'com.u.About2': [
            ('java.lang.String', 'name')
        ],
        'com.u.a$n': [
            ('java.lang.String', 'name'),
            ('com.def.Location', 'loc'),
            ('boolean', 'p3'),
        ],
        'com.def.Location': [
            ('java.lang.String', 'name'),
            ('java.lang.String', 'value')
        ],
        'com.u.LatLng': [
            ('java.lang.String', 'lt'),
            ('java.lang.String', 'ln'),
            ('java.lang.String', 'icq'),
            ('com.u.a$n', 'about')
        ],
        'com.interfocusllc.patpat.bean.LifePersonalResponse$UserInfo': [
            ('java.lang.String', 'name'),
            ('java.lang.String', 'avatar'),
            ('java.lang.String', 'email'),
            ('java.lang.String', 'first_name'),
            ('int', 'ispatpat'),
            ('java.lang.String', 'last_name'),
            ('com.interfocusllc.patpat.bean.LifePersonalResponse', 'this$0'),
            ('long', 'user_id'),
            ('com.u.LatLng', 'l'),
            ('java.lang.Boolean', 'facebook'),
            ('java.lang.String', 'facebook'),
            ('boolean', 'acctMgtActionButtonEligible'),
            ('boolean', 'allowTransferFrom'),
            ('boolean', 'allowTransferTo')
        ]
    }
    res = batch_judge(input_data)
    # 打印结果
    print()
    print()
    print()
    for k, v in res.items():
        if v:
            print('[Identified Classes]', k)
            for item in v:
                print('   ', item)



if __name__=='__main__':
    pass

    test()

    # Input format of batch_judge():
    # input_data = {
    #     'class_name0': [
    #         ('field_type0', 'field_name0'),
    #         ('field_type1', 'field_name1'),
    #         ...
    #     ],
    #     'class_name1': [
    #         ('field_type2', 'field_name2'),
    #         ('field_type3', 'field_name3'),
    #         ...
    #     ]
    # }
    # res = batch_judge(input_data)


    # Output format of batch_judge()
    # {
    #     'class_name0': None
    #     'class_name1': [
    #         (field_flag0, field_name0, privacy_category0),
    #         (field_flag1, field_name1, privacy_category1),
    #         ...
    #     ]
    # }


