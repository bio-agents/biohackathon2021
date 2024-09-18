import json, re
from boltons.iterutils import remap
from collections import defaultdict
import operator


def read_local_agents():
    with open('../RScriptVeit/bio.agentsFullDump.json') as f:
      return json.load(f)


def clean_agents_list(agents_list):
    drop_false = lambda path, key, value: bool(value)
    return [remap(agent, visit=drop_false) for agent in agents_list]

def has_data_with_format(agent): 
    for f in agent.get('function',[]):
        for i in f.get('input',[]):
            if i.get('data') and i['data']['uri'].lower() == 'http://edamontology.org/data_0006' and i.get('format'):
                return True
        for o in f.get('output',[]):
            if o.get('data') and o['data']['uri'].lower() == 'http://edamontology.org/data_0006' and o.get('format'):
                return True
    return False

all_agents = clean_agents_list(read_local_agents())

for agent in all_agents:
    if has_data_with_format(agent):
        print(agent['bioagentsID'])
