import datetime
import os
import subprocess
import time

WORKDIR = "/home/sanu/rickastlee/"
LEMONAGE = WORKDIR + "LemonageOS/"
SOURCE = WORKDIR + "android/lineage/"
COMMITS = LEMONAGE + "commits/"
UNIFIED = LEMONAGE + "commitsunified/"
BACKUP = LEMONAGE + "sourcebackup/"
DELETECOMMITS = LEMONAGE + "deletecommits/"
DELETEBACKUP = LEMONAGE + "deletesourcebackup/"

def delete_old_dirs():
    subprocess.run(["rm", "-rf", UNIFIED])
    subprocess.run(["rm", "-rf", BACKUP])
    subprocess.run(["rm", "-rf", DELETEBACKUP])
def create_dirs_in_unified_and_backup():
    abs_dir_paths = []
    rel_dir_paths = []

    for path, dirs, files in os.walk(COMMITS):
        for subdir in dirs:
            abs_dir_paths.append(os.path.join(path, subdir))

    for i in abs_dir_paths:
        rel_path = ""
        dir_list = i.split("/")
        if len(dir_list) > 4:
            for i in range(4, len(dir_list)):
                rel_path += dir_list[i] + "/"
            if not rel_path in rel_dir_paths:
                rel_dir_paths.append(rel_path)

    for i in rel_dir_paths:
        if not os.path.exists(UNIFIED + i):
            subprocess.run(["mkdir", "-p", UNIFIED + i])
        if not os.path.exists(BACKUP + i):
            subprocess.run(["mkdir", "-p", BACKUP + i])
def copy_files_to_unified():
    abs_file_paths = []
    all_mod_times = {}
    newest_mod_times = {}

    for path, subdirs, files in os.walk(COMMITS):
        for name in files:
            abs_file_paths.append(os.path.join(path, name))

    for i in abs_file_paths:
        all_mod_times[i] = os.path.getmtime(i)

    for i in abs_file_paths:
        full_path = i
        rel_path = ""
        dir_list = i.split("/")
        for i in range(4, len(dir_list)):
            rel_path += dir_list[i] + "/"
        rel_path = rel_path[:-1]
        curr_mod_time = os.path.getmtime(full_path)
        unified_path = UNIFIED + rel_path
        if os.path.exists(unified_path):
            if curr_mod_time > newest_mod_times[rel_path]:
                subprocess.run(["cp", full_path, unified_path])
                newest_mod_times[rel_path] = curr_mod_time
        else:
            subprocess.run(["cp", full_path, unified_path])
            newest_mod_times[rel_path] = curr_mod_time
def create_dirs_in_deletebackup():
    abs_dir_paths = []
    rel_dir_paths = []

    for path, dirs, files in os.walk(DELETECOMMITS):
        for subdir in dirs:
            abs_dir_paths.append(os.path.join(path, subdir))

    for i in abs_dir_paths:
        rel_path = ""
        dir_list = i.split("/")
        if len(dir_list) > 4:
            for i in range(4, len(dir_list)):
                rel_path += dir_list[i] + "/"
            if not rel_path in rel_dir_paths:
                rel_dir_paths.append(rel_path)

    for i in rel_dir_paths:
        if not os.path.exists(DELETEBACKUP + i):
            subprocess.run(["mkdir", "-p", DELETEBACKUP + i])
def create_dirs_in_source():
    abs_dir_paths = []
    rel_dir_paths = []
    dirs_to_create = []
    dirs_to_delete = []
    out1 = open("DIRS_TO_CREATE.txt", "w")
    out2 = open("DIRS_TO_DELETE.txt", "w")

    for path, dirs, files in os.walk(UNIFIED):
        for subdir in dirs:
            abs_dir_paths.append(os.path.join(path, subdir))

    for i in abs_dir_paths:
        rel_path = ""
        dir_list = i.split("/")
        if len(dir_list) > 2:
            for i in range(2, len(dir_list)):
                rel_path += dir_list[i] + "/"
            if not rel_path in rel_dir_paths:
                rel_dir_paths.append(rel_path)

    for i in rel_dir_paths:
        i = i[:-1]
        if not os.path.exists(SOURCE + i):
            if not i in dirs_to_create:
                dirs_to_create.append(i)
            while not os.path.exists(SOURCE + i):
                curr_dir = i[i.rindex("/") + 1 : ]
                i = i[ : i.rindex("/")]
            i = i + "/" + curr_dir
            if not i in dirs_to_delete:
                dirs_to_delete.append(i)

    for i in dirs_to_create:
        subprocess.run(["mkdir", "-p", SOURCE + i])
        out1.write(i + "\n")

    for i in dirs_to_delete:
        out2.write(i + "\n")
def copy_files_to_source():
    abs_file_paths = []

    for path, subdirs, files in os.walk(UNIFIED):
        for name in files:
            abs_file_paths.append(os.path.join(path, name))

    for i in abs_file_paths:
        original_path = i
        rel_path = ""
        dir_list = i.split("/")
        for i in range(2, len(dir_list)):
            rel_path += dir_list[i] + "/"
        rel_path = rel_path[:-1]
        source_path = SOURCE + rel_path
        if os.path.exists(source_path):
            subprocess.run(["cp", source_path, BACKUP + rel_path])
        subprocess.run(["cp", original_path, source_path])
def delete_files_from_source():
    abs_file_paths = []

    for path, subdirs, files in os.walk(DELETECOMMITS):
        for name in files:
            abs_file_paths.append(os.path.join(path, name))

    for i in abs_file_paths:
        full_path = i
        rel_path = ""
        dir_list = i.split("/")
        for i in range(4, len(dir_list)):
            rel_path += dir_list[i] + "/"
        rel_path = rel_path[:-1]
        source_path = SOURCE + rel_path
        if os.path.exists(source_path):
            subprocess.run(["cp", source_path, DELETEBACKUP + rel_path])
        subprocess.run(["rm", "-f", source_path])

delete_old_dirs()
create_dirs_in_unified_and_backup()
copy_files_to_unified()
create_dirs_in_deletebackup()
create_dirs_in_source()
copy_files_to_source()
delete_files_from_source()
print("\nLocal commits have been merged\n")