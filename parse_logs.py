



import json
import os
import PrivacyDiscoverer

ude_logs_path = "ude_logs"
ude_logs_formatted_path = "ude_logs_formatted"
if not os.path.exists(ude_logs_formatted_path):
    os.mkdir(ude_logs_formatted_path)
ude_logs_with_privacy_path = "ude_logs_with_privacy"
if not os.path.exists(ude_logs_with_privacy_path):
    os.mkdir(ude_logs_with_privacy_path)


def read_lines(file_path):
    lines = None
    with open(file_path, mode="r", encoding="utf-8") as f:
        lines = f.readlines()
    return lines


def write_lines(file_path, lines):
    with open(file_path, mode="w", encoding="utf-8") as f:
        f.writelines(lines)

 
def append_line_to_file(file_path, content):
    with open(file_path, 'a') as f:
        f.write(str(content) + "\n")


# 排除掉一些请求和结果不对应的情况
def get_formatted_lines(lines: list):
    status = 0
    backward_privacy = 0
    forward_privacy = 0
    backward_lines = []
    forward_lines = []
    res = []
    for line_index, line in enumerate(lines):
        line: str = line.strip()
        if line.startswith("[Backward]"):
            status = 1
        elif line.startswith("[Forward]"):
            status = 2
        elif line == "":
            if status != 0:
                status = 0
                if backward_lines and forward_lines:
                # if forward_lines:
                    res.append("[Backward]" + "\n")
                    for format_line in backward_lines:
                        res.append(format_line + "\n")
                    res.append("[Forward]" + "\n")
                    for format_line in forward_lines:
                        res.append(format_line + "\n")
                    res.append("\n")
                    backward_lines = []
                    forward_lines = []
            # 判断是否配对成功
        else:
            if status == 1:
                backward_lines.append(line)
            elif status == 2:
                forward_lines.append(line)
    return res


def format_requests_and_responses():
    root_log_dir_path = ude_logs_path
    categories = os.listdir(root_log_dir_path)
    for cate_idx, category in enumerate(categories):
        category_path = os.path.join(root_log_dir_path, category)
        if os.path.isfile(category_path):
            continue
        formatted_cate_path = os.path.join(ude_logs_formatted_path, category)
        if not os.path.exists(formatted_cate_path):
            os.mkdir(formatted_cate_path)
        log_names = os.listdir(category_path)
        for log_idx, log_name in enumerate(log_names):
            if log_name.endswith("_field_info.txt"):
                continue
            if not log_name.endswith(".txt"):
                continue
            formatted_log_path = os.path.join(formatted_cate_path, log_name)
            if os.path.exists(formatted_log_path):
                continue
            print("Format", category, log_name)
            log_path = os.path.join(category_path, log_name)
            lines = read_lines(log_path)
            formatted_lines = get_formatted_lines(lines)

            if formatted_lines:
                write_lines(formatted_log_path, formatted_lines)


def filter_requests_with_privacy():
    root_log_dir_path = ude_logs_formatted_path
    category_names = os.listdir(root_log_dir_path)
    for cate_index, category in enumerate(category_names):
        category_path = os.path.join(root_log_dir_path, category)
        if os.path.isfile(category_path):
            continue
        ude_logs_with_privacy_category_path = os.path.join(ude_logs_with_privacy_path, category)
        if not os.path.exists(ude_logs_with_privacy_category_path):
            os.mkdir(ude_logs_with_privacy_category_path)
        log_names = os.listdir(category_path)
        for log_index, log_name in enumerate(log_names):
            if not log_name.endswith(".txt"):
                continue
            res_path = os.path.join(ude_logs_with_privacy_category_path, log_name)
            if os.path.exists(res_path):
                continue
            print("Filter privacy", category, log_name)
            res_lines = []
            appid = log_name[:-len(".txt")]
            log_path = os.path.join(category_path, log_name)
            lines = read_lines(log_path)
            all_field_info = load_all_field_info(category, appid)
            all_requests_in_lines = get_requests(lines)
            res_recorder = {}
            for single_request_in_lines in all_requests_in_lines:
                single_request = parse_single_request(single_request_in_lines)
                batch_info_in_current_request = {}
                resp_info_items = single_request["[Forward]"]
                string_consts_fields, classes = get_key_strings_and_classes(resp_info_items)

                batch_info_in_current_request["JsonObject"] = string_consts_fields # 用所有相关的KeyString组成一个类
                related_classes = set()
                for each_class in classes:
                    if each_class not in res_recorder and each_class not in related_classes:
                        if each_class in all_field_info:
                            related_classes.add(each_class)
                all_classes = get_all_related_classes(list(related_classes), all_field_info) # 获取所有相关的类

                # 取出当前请求涉及到的类信息
                for class_name in all_classes:
                    if class_name in all_field_info:
                        batch_info_in_current_request[class_name] = all_field_info[class_name]

                # 隐私分析
                res = PrivacyDiscoverer.judge_privacy.batch_judge(batch_info_in_current_request)
                
                contains_privacy = False
                for class_name, field_items in res.items():
                    res_recorder[class_name] = field_items
                    if field_items:
                        for item in field_items:
                            if item[0]:
                                print(item)
                                contains_privacy = True
                if contains_privacy:
                    res_lines.extend(single_request_in_lines)

            if res_lines:
                write_lines(res_path, res_lines)



def load_all_field_info(category, appid):
    all_field_info = {}
    category_path = os.path.join("ude_logs", category)
    field_info_path = os.path.join(category_path, appid+"_field_info.txt")
    if os.path.exists(field_info_path):
        field_lines = read_lines(field_info_path)
        for field_line in field_lines:
            field_line = field_line.strip()
            soot_field_str = field_line[len("ClassField:"):]
            s0, s1, s2 = soot_field_str.split(" ")
            class_name = s0[1:-1]
            field_type = s1
            field_name = s2[:-1]
            if class_name not in all_field_info:
                all_field_info[class_name] = []
            all_field_info[class_name].append((field_type, field_name))
    return all_field_info


def get_key_strings_and_classes(items: list):
    # 返回字符串常量构造field的对象
    status = 0
    key_strings = []
    classes = []
    for item_index, item in enumerate(items):
        if item.startswith("Class:"):
            class_name = item[len("Class:"):]
            classes.append(class_name)
        elif item.startswith("KeyString:"):
            key_string = item[len("KeyString:"):]
            key_strings.append(('java.lang.String', key_string))
    return key_strings, classes


def get_all_related_classes(original_classes: list, all_field_info):
    res = set()
    q = original_classes
    while q:
        class_name = q.pop(0)
        res.add(class_name)
        if class_name in all_field_info:
            for field_item in all_field_info[class_name]:
                field_type = field_item[0]
                if field_type in all_field_info and field_type not in res:
                    q.append(field_type)
    return res


def get_requests(lines: list):
    status = 0
    backward_privacy = 0
    forward_privacy = 0
    backward_lines = []
    forward_lines = []
    res = [] # 按照请求数据排列的结果
    for line_index, line in enumerate(lines):
        line: str = line.strip()
        if line.startswith("[Backward]"):
            status = 1
        elif line.startswith("[Forward]"):
            status = 2
        elif line == "":
            if status != 0:
                status = 0
                if backward_lines and forward_lines:
                # if forward_lines:
                    current_request = []
                    current_request.append("[Backward]" + "\n")
                    for format_line in backward_lines:
                        current_request.append(format_line + "\n")
                    current_request.append("[Forward]" + "\n")
                    for format_line in forward_lines:
                        current_request.append(format_line + "\n")
                    current_request.append("\n")
                    res.append(current_request)
                    backward_lines = []
                    forward_lines = []
        else:
            if status == 1:
                backward_lines.append(line)
            elif status == 2:
                forward_lines.append(line)
    return res


def parse_single_request(lines):
    backward_lines = []
    forward_lines = []
    status = 0
    for line_index, line in enumerate(lines):
        line: str = line.strip()
        if line.startswith("[Backward]"):
            status = 1
        elif line.startswith("[Forward]"):
            status = 2
        elif line == "":
            if status != 0:
                status = 0
                if backward_lines and forward_lines:
                # if forward_lines:
                    current_request = {
                        "[Backward]": [],
                        "[Forward]": []
                    }
                    for format_line in backward_lines:
                        current_request["[Backward]"].append(format_line)
                    for format_line in forward_lines:
                        current_request["[Forward]"].append(format_line)
                    return current_request
        else:
            if status == 1:
                backward_lines.append(line)
            elif status == 2:
                forward_lines.append(line)


def count():
    root_log_dir_path = ude_logs_with_privacy_path
    category_names = os.listdir(root_log_dir_path)
    for cate_index, category in enumerate(category_names):
        category_path = os.path.join(root_log_dir_path, category)
        log_names = os.listdir(category_path)
        print(category, len(log_names))


def run():
    # count()
    format_requests_and_responses()
    filter_requests_with_privacy()


if __name__=="__main__":
    pass
    run()





"""
耗时: 70452秒
耗时: 50633秒
耗时: 60002秒
耗时: 69203秒
耗时: 66223秒
耗时: 65533秒
耗时: 55603秒
耗时: 60001秒
耗时: 70012秒
耗时: 50503秒
耗时: 53362秒
耗时: 64867秒
耗时: 60983秒
耗时: 60928秒
耗时: 69824秒
耗时: 69393秒
耗时: 60652秒
耗时: 70432秒
耗时: 55072秒
耗时: 67452秒
耗时: 59536秒
耗时: 54696秒
耗时: 74718秒
耗时: 73024秒
耗时: 74626秒
耗时: 64166秒
耗时: 76699秒
耗时: 85037秒
耗时: 53343秒
"""