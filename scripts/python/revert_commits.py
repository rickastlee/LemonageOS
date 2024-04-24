import os
import random
import subprocess

WORKDIR = "/home/sanu/rickastlee/"
LEMONAGE = WORKDIR + "LemonageOS/"

SOURCE = WORKDIR + "android/lineage/"
UNIFIED = LEMONAGE + "commitsunified/"
BACKUP = LEMONAGE + "sourcebackup/"
DELETEBACKUP = LEMONAGE + "deletesourcebackup/"

def abort_if_ran_by_accident():
    print("To help avoid reverting commits by accident, you must provide the correct answer for the following question")
    operator = random.randint(0, 1) 
    number1 = random.randint(1000, 1000000)
    number2 = random.randint(1000, 1000000)
    if operator == 0:
        answer = number1 + number2
        guess = int(input("{} + {} ? ".format(number1, number2)))
    else:
        answer = number1 * number2
        guess = int(input("{} * {} ? ".format(number1, number2)))
    if guess != answer:
        print("\nAborting process...")
        exit()
def restore_modified_files_to_source_and_delete_new_ones():
    abs_file_paths = []

    for path, subdirs, files in os.walk(UNIFIED):
        for name in files:
            abs_file_paths.append(os.path.join(path, name))

    for i in abs_file_paths:
        full_path = i
        rel_path = ""
        dir_list = i.split("/")
        for i in range(2, len(dir_list)):
            rel_path += dir_list[i] + "/"
        rel_path = rel_path[:-1]
        source_path = SOURCE + rel_path
        if os.path.exists(BACKUP + rel_path):
            subprocess.run(["cp", BACKUP + rel_path, source_path])
        else:
            if os.path.exists(source_path):
                subprocess.run(["rm", "-f", source_path])    
def restore_deleted_files_to_source():
    abs_file_paths = []
    rel_file_paths_skipped = []

    ip = open("files_not_to_restore", "r")
    for i in ip.readlines():
        rel_file_paths_skipped.append(i.strip())

    for path, subdirs, files in os.walk(DELETEBACKUP):
        for name in files:
            abs_file_paths.append(os.path.join(path, name))

    for i in abs_file_paths:
        full_path = i
        rel_path = ""
        dir_list = i.split("/")
        for i in range(2, len(dir_list)):
            rel_path += dir_list[i] + "/"
        rel_path = rel_path[:-1]
        if not rel_path in rel_file_paths_skipped:
            subprocess.run(["cp", full_path, SOURCE + rel_path])
def delete_dirs_in_source():
    dirs_to_delete = []
    ip = open ("DIRS_TO_DELETE.txt", "r")

    for i in ip.readlines():
        dirs_to_delete.append(i.strip())

    for i in dirs_to_delete:
        subprocess.run(["rm", "-r", SOURCE + i])

#abort_if_ran_by_accident()
restore_modified_files_to_source_and_delete_new_ones()
restore_deleted_files_to_source()
delete_dirs_in_source()
print("\nLocal commits have been reverted\n")