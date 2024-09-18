import json

import requests


def download_whole_bioagents():
    url: str = "https://bio.agents/api/t/?format=json"
    agents: list = []
    next_page: str = ""
    page_number: int = 1
    while next_page is not None:
        print(f"Page number: {page_number}")
        resp = requests.get(url + next_page.replace("?", "&"))
        resp.raise_for_status()
        data = resp.json()
        agents.extend(data["list"])
        next_page = data["next"]
        page_number += 1

    print(len(agents))

    with open("Resources/FullAgents.json", "w") as f:
        f.write(json.dumps(agents))


def get_certain_agents():
    with open("Resources/FullCollection/FullAgents.json", "r") as f:
        agents = json.load(f)

    with open("Resources/electron_microscopy_domain.txt", "r") as f:
        agents_ids = [agent_id.lower().strip() for agent_id in f.readlines()]
    agent_collection = [agent for agent in agents if agent["bioagentsID"].lower() in agents_ids]

    with open("Resources/ElectronMicroscopyAgents.json", "w") as f:
        f.write(json.dumps(agent_collection))



def main():
    get_certain_agents()


if __name__ == "__main__":
    main()
    pass
