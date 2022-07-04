#!/usr/bin/python
#-- coding:utf8 --


import string
import json


def mutate(original_values, input_type):
    lower_values = original_values
    if input_type == 'java.lang.String':
        lower_values = [item.lower() for item in original_values]
    assert len(lower_values)==len(set(lower_values)), "repeat"

    if input_type == 'boolean':
        return original_values

    res = []

    for original_val in original_values:
        res.append(str(original_val))
        c_list = list(str(original_val))

        for index, c in enumerate(c_list):
            c_list[index] = mutate_down(c)
            mutated_str = ''.join(c_list)
            if mutated_str not in res:
                res.append(mutated_str)

            c_list[index] = mutate_up(c)
            mutated_str = ''.join(c_list)
            if mutated_str not in res:
                res.append(mutated_str)

            c_list[index] = c

        if input_type=='int':
            for index, r in enumerate(res):
                res[index] = int(r)
        elif input_type=='float':
            for index, r in enumerate(res):
                res[index] = float(r)
    check_repeat(res)
    print(input_type)
    return res
    # print(json.dumps(res))


def check_repeat(mutate_list):
    # res = []
    # for item in mutate_list:
    #     try:
    #         tranformed_item = float(item)
    #         res.append(tranformed_item)
    #     except:
    #         res.append(item)
    if len(mutate_list) != len(set(mutate_list)):
        print(mutate_list)
        print(len(mutate_list), len(set(mutate_list)))
    assert len(mutate_list)==len(set(mutate_list)), "repeat"

def mutate_down(c):
    if c in string.digits or c in string.ascii_letters:
        if c == '0' or c == 'A' or c == 'a':
            return c
        return chr(ord(c)-1)
    return c


def mutate_up(c):
    if c in string.digits or c in string.ascii_letters:
        if c == '9' or c == 'Z' or c == 'z':
            return c
        return chr(ord(c)+1)
    return c

def to_java_string(res_type, res_list):
    res_list = [str(i) for i in res_list]
    prefix = 'inputs[] = {'
    suffix = '};'
    content = ''
    if res_type == 'java.lang.String':
        content = '\"' + '\", \"'.join(res_list) + '\"'
    elif res_type == 'float':
        content = 'f, '.join(res_list) + 'f'
    else:
        content = ', '.join(res_list)
        if res_type == 'boolean':
            content = content.lower()

    return '{}{}{}'.format(prefix, content, suffix)

def main():
    content = ''
    with open("lexicon_values.json", 'r', encoding="utf-8") as f:
        content = json.load(f)

    # print(content)
    res = {}
    for privacy_category in content:
        if not content[privacy_category]:
            res[privacy_category] = None
            continue
        res[privacy_category] = []
        for value_item in content[privacy_category]:
            mutated_value_item = {}
            mutated_value_item["lexicons"] = value_item["lexicons"]
            mutated_value_item["values"] = {}
            for data_type, init_values in value_item["values"].items():
                mutated_value_item["values"][data_type] = to_java_string(data_type, mutate(init_values, data_type))
            res[privacy_category].append(mutated_value_item)

    f = open('assigned_values.json', 'w')
    f.write(json.dumps(res))
    f.close()

def get_type_match():
    content = ''
    with open("assigned_values.json", 'r', encoding="utf-8") as f:
        content = json.load(f)

    res = {}
    for cate in content:
        res[cate] = []
        for tp in content[cate]:
            res[cate].append(tp)
            if tp == 'byte':
                res[cate].append('java.lang.Byte')
            elif tp == 'short':
                res[cate].append('java.lang.Short')
            elif tp == 'int':
                res[cate].append('java.lang.Integer')
            elif tp == 'long':
                res[cate].append('java.lang.Long')
            elif tp == 'float':
                res[cate].append('java.lang.Float')
            elif tp == 'double':
                res[cate].append('java.lang.Double')
            elif tp == 'boolean':
                res[cate].append('java.lang.Boolean')
            elif tp == 'java.lang.String':
                res[cate].append('java.lang.CharSequence')

    # print(json.dumps(res))

if __name__=='__main__':
    # print(mutate(["20000101", "2000-01-01"], "java.lang.String"))

    main()
    # get_type_match()

