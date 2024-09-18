"""
The scripts for calculating statistics for the EDAM terms for the terms.

"""
import iteragents
from collections import defaultdict
from datetime import datetime

from ._utilities import clean_and_filter_agent_list


def calculate_edam_term_statistics(agents: list, term_type: str, index_list: dict,
                                   upper_time_limit: datetime = datetime.today(), output_ids: bool = False) -> dict:
    """
    Calculate the statistics for EDAM terms.

    :param agents: The agent list.
    :param term_type: The term type to calculate statistics for.
    :param index_list: The index list for the terms.
    :param upper_time_limit: Calculate the statistics for agents added up to the time limit.
        Default: datetime.datetime.today().
    :param output_ids: Indicate whether the ids should be in the output. Default: False.
    :return: The dictionary with the terms, the IDs and counts for strict (Only the specific term)
        and total (for parent terms).
    """

    agents = clean_and_filter_agent_list(raw_agents=agents, upper_time_limit=upper_time_limit)

    # Create the dictionary to hold the topic statistics with the default fields.
    temp_statistics = defaultdict(
        lambda: {"name": "", "depth": -1,
                 "strict_ids": set(), "total_ids": set(),
                 "strict_count": 0, "total_count": 0})

    term_type = term_type.lower()

    agent_terms: dict = _extract_terms(agents=agents, term_type=term_type)
    # Loop over the agents and the topics
    for agentID in agent_terms:
        for term in agent_terms[agentID]:
            statistics = _add_terms(stats=temp_statistics, term=term, agent_id=agentID, index_list=index_list)

    # Create the final statistics dict
    statistics: dict = {}
    statistics["date"] = upper_time_limit.isoformat(timespec="seconds")
    statistics[term_type]: defaultdict = defaultdict(
        lambda: {"name": "", "depth": -1,
                 "strict_ids": [], "total_ids": [],
                 "strict_count": 0, "total_count": 0})

    # Loop over the statistics
    for term_id in temp_statistics:
        statistics[term_type][term_id]["name"] = temp_statistics[term_id]["name"]
        statistics[term_type][term_id]["depth"] = temp_statistics[term_id]["depth"]
        statistics[term_type][term_id]["total_count"] = len(temp_statistics[term_id]["total_ids"])
        statistics[term_type][term_id]["strict_count"] = len(temp_statistics[term_id]["strict_ids"])

        if output_ids:
            statistics[term_type][term_id]["total_ids"] = list(temp_statistics[term_id]["total_ids"])
            statistics[term_type][term_id]["strict_ids"] = list(temp_statistics[term_id]["strict_ids"])

    return statistics


def _add_terms(stats: dict, term: dict, agent_id: str, index_list: dict) -> dict:
    """
    Add term to the statistics.

    :param stats: The statistics dictionary.
    :param term: The EDAM term.
    :param agent_id: The bio.agents ID.
    :param index_list: The index list.
    :return: The statistics dictionary.
    """
    # Get the term id
    term_id = term["uri"].replace("http://edamontology.org/", "")
    # Add to the id lists
    stats[term_id]["strict_ids"].add(agent_id)
    stats[term_id]["total_ids"].add(agent_id)

    # Add topic name and depth if not added
    stats = _add_term_info(stats=stats, term_id=term_id, index_list=index_list)

    # Go through the branches
    for branch_term in _get_branch_terms(term_id=term_id, term_index=index_list):
        # Add topic name and depth if not added
        stats = _add_term_info(stats=stats, term_id=branch_term, index_list=index_list)
        stats[branch_term]["total_ids"].add(agent_id)

    return stats


def _get_branch_terms(term_id: str, term_index: dict) -> list:
    """
    Get the branch terms.

    :param term_id: The term ID.
    :param term_index: The term index list.
    :return: The list of branch terms.
    """
    terms: list = []
    if term_id in term_index:
        for branch in term_index[term_id]["path"]:
            terms.extend(branch["key"].split("||"))
    return terms


def _add_term_info(stats: dict, term_id: str, index_list: dict) -> dict:
    """
    Add term information, if none exists.

    :param stats: The statistics dictionary.
    :param term_id: The EDAM term.
    :param index_list: The index list.
    :return: The statistics dictionary.
    """
    if term_id in index_list:
        if stats[term_id]["name"] == "":
            stats[term_id]["name"] = index_list[term_id]["name"]

            # Calculate the term depth
            path_depths: list = []
            for path in index_list[term_id]["path"]:
                path_depths.append(len(path["key"].split("||")))
            stats[term_id]["depth"] = min(path_depths) - 1  # Ensure topic is depth 0 = Root

    return stats


def _extract_terms(agents: list, term_type: str) -> dict:
    """
    Extract terms from the agents.

    :param agents: The list of agents.
    :param term_type: The term type.
    :return: The dictionary with the agent ID and the terms.
    """
    term_type = term_type.capitalize()

    if term_type == "Topic":
        return _extract_edam_topics(agents=agents)
    elif term_type == "Operation":
        return _extract_edam_operation(agents=agents)
    elif term_type == "Format":
        return _extract_edam_format(agents=agents)
    elif term_type == "Data":
        return _extract_edam_data(agents=agents)
    else:
        raise ValueError(f"The term type '{term_type}' is not valid. Must be 'Topic', 'Operation', 'Format', or"
                         f"'Data'.")


def _extract_edam_topics(agents: list) -> dict:
    """
    Get the EDAM topics for each agent.

    :param agents: The list of agents.
    :return: The dictionary with the agent id and the topics.
    """
    terms: defaultdict = defaultdict(lambda: [])

    for agent in agents:
        terms[agent["bioagentsID"]].extend([topic for topic in agent["topic"]])

    return terms


def _extract_edam_operation(agents: list) -> dict:
    """
    Get the EDAM operation for each agent.

    :param agents: The list of agents.
    :return: The dictionary with the agent id and the operations.
    """
    terms: defaultdict = defaultdict(lambda: [])

    for agent in agents:
        terms[agent["bioagentsID"]].extend(*[function["operation"] for function in agent["function"]])

    return terms


def _extract_edam_format(agents: list) -> dict:
    """
    Get the EDAM format for each agent.

    :param agents: The list of agents.
    :return: The dictionary with the agent id and the formats.
    """
    terms: defaultdict = defaultdict(lambda: [])

    for agent in agents:
        terms[agent["bioagentsID"]].extend(iteragents.chain(*_get_inputs_outputs_info(agent=agent, term_type="format")))
    return terms


def _extract_edam_data(agents: list) -> dict:
    """
    Get the EDAM data for each agent.

    :param agents: The list of agents.
    :return: The dictionary with the agent id and the topics.
    """
    terms: defaultdict = defaultdict(lambda: [])
    for agent in agents:
        terms[agent["bioagentsID"]].extend(_get_inputs_outputs_info(agent=agent, term_type="data"))
    return terms


def _get_inputs_outputs_info(agent: dict, term_type: str) -> list:
    """
    Get the inputs and outputs for a agent.

    :param agent: The agent dict.
    :param term_type: The term type.
    :return: The list with the specific terms.
    """
    terms: list = []

    for function in agent["function"]:
        if "input" in function:
            for i in function["input"]:
                if term_type in i:
                    terms.append(i[term_type])
        if "output" in function:
            for o in function["output"]:
                if term_type in o:
                    terms.append(o[term_type])

    return list(terms)
