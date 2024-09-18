"""
The test script for the bioagents_statistic package.
Used for showing plots etc.

If script is to run on other machines, please be aware of the file path.
"""
import requests
import json

from requests import Response

from bioagents_statistics import calculate_general_statistics
from bioagents_statistics import calculate_edam_term_statistics


def _get_index_list(term_type: str):
    """
    Get the index list.

    :param term_type: The EDAM term type.
    :return: The index list.
    """
    if term_type.capitalize() not in ["Topic", "Operation", "Format", "Data"]:
        raise ValueError(f"The term type '{term_type}' is not valid. Must be 'Topic', 'Operation', 'Format', or"
                         f"'Data'.")

    resp: Response = requests.get(f"https://bio.agents/api/o/index_EDAM_{term_type}?format=json")
    resp.raise_for_status()
    index_list: dict = resp.json()["data"]
    return index_list


def main():
    """
    The main entry point of the script.
    """
    with open("Agents.json", "r") as f:
        agents = json.load(f)

    stats = calculate_general_statistics(agents=agents)

    # print(json.dumps(stats, indent=4))

    term_stats = calculate_edam_term_statistics(agents=agents, term_type="topic", index_list=_get_index_list("topic"))
    print(json.dumps(term_stats, indent=4))
    print("\n" * 2)

    exit()
    term_stats = calculate_edam_term_statistics(agents=agents, term_type="operation",
                                                index_list=_get_index_list("operation"))
    print(json.dumps(term_stats, indent=4))
    print("\n" * 2)

    term_stats = calculate_edam_term_statistics(agents=agents, term_type="format", index_list=_get_index_list("format"))
    print(json.dumps(term_stats, indent=4))
    print("\n" * 2)

    term_stats = calculate_edam_term_statistics(agents=agents, term_type="data", index_list=_get_index_list("data"))
    print(json.dumps(term_stats, indent=4))


if __name__ == "__main__":
    main()
