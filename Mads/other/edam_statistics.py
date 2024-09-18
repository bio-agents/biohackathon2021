from typing import Tuple

import matplotlib.pyplot as plt
import pandas as pd
import requests
from boltons.iterutils import remap
from collections import defaultdict
import json
import seaborn as sns

from requests import Response


def main():
    plt.style.use("ggplot")
    # Read the agents
    collection_name: str = "Electron microscopy"
    with open(f"Resources/{collection_name.title().replace(' ', '')}Collection/Agents.json", "r", encoding="utf8") as f:
        agents = json.load(f)

    calculate_statistics(raw_agents=agents, collection_name=collection_name)


def calculate_statistics(raw_agents: list, collection_name: str):
    """
    Calculate the statistics (Main function).

    :param raw_agents: The raw list of agents.
    :param collection_name: The name of the collection.
    :return:
    """
    # Clean the list
    drop_false = lambda path, key, value: bool(value)
    agents = remap(raw_agents, visit=drop_false)

    # Calculate the EDAM term statistics
    topic_stats = calculate_edam_topic_statistics(agents=agents)

    # Create the collection folder
    collection_folder_name: str = f"{collection_name.title().replace(' ', '')}Collection"

    # with open(f"Resources/{collection_folder_name}/Topics.json", "w") as f:
    #    f.write(json.dumps(topic_stats, indent=4, cls=SetEncoder))

    topic_stats_df, reduced_topic_stats_df = _create_topics_dataframe(topic_stats, collection_folder_name,
                                                                      cutoff_value=0, min_depth=2)

    _create_topics_statistics_plot(topic_stats_df=topic_stats_df, collection_name=collection_name)
    _create_partial_topics_statistics_plot(topic_stats_df=topic_stats_df, count_type="Total",
                                           collection_name=collection_name)
    _create_partial_topics_statistics_plot(topic_stats_df=topic_stats_df, count_type="Strict",
                                           collection_name=collection_name)

    _create_topics_statistics_plot(topic_stats_df=reduced_topic_stats_df, collection_name=collection_name,
                                   min_cutoff=None, min_cutoff_unit="total", min_depth=2)
    _create_partial_topics_statistics_plot(topic_stats_df=reduced_topic_stats_df, count_type="Total",
                                           collection_name=collection_name, min_cutoff=None, min_cutoff_unit="Total",
                                           min_depth=2)
    _create_partial_topics_statistics_plot(topic_stats_df=reduced_topic_stats_df, count_type="Strict",
                                           collection_name=collection_name, min_cutoff=None, min_cutoff_unit="Total",
                                           min_depth=2)


def calculate_edam_topic_statistics(agents: list) -> dict:
    """
    Calculate the statistics for EDAM topics.

    :param agents: The agent list.
    :return: The dictionary with the terms, the IDs and counts for strict (Only the specific term)
        and total (for parent terms).
    """
    # Create the dictionary to hold the topic statistics with the default fields.
    statistics = defaultdict(
        lambda: {"name": "", "depth": -1,
                 "strict_ids": set(), "total_ids": set(),
                 "strict_count": 0, "total_count": 0})

    # Get the index list
    index_list: dict = _get_index_list(term_type="topic")

    # Loop over the agents and the topics
    for agent in agents:
        if "topic" in agent:
            for term in agent["topic"]:
                statistics = _add_terms(stats=statistics, term=term, agent_id=agent["bioagentsID"], index_list=index_list)

    # Loop over the statistics
    for term in statistics.keys():
        statistics[term]["strict_count"] = len(statistics[term]["strict_ids"])
        statistics[term]["total_count"] = len(statistics[term]["total_ids"])

    return statistics


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


def _add_terms(stats: dict, term: dict, agent_id: dict, index_list: dict) -> dict:
    """
    Add term to the statistics.

    :param stats: The statistics dictionary.
    :param term: The EDAM term.
    :param agent_id: The bio.agents ID.
    :param index_list: The index list.
    :return: The statistics dictionary.
    """
    # Get the term id
    term_id = term["uri"].split("/")[3]  # Example split: ['http:', '', 'edamontology.org', 'topic_3577']
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

            path_depths: list = []
            for path in index_list[term_id]["path"]:
                path_depths.append(len(path["key"].split("||")))

            # stats[term_id]["depth"] = len(index_list[term_id]["path"][0]["key"].split("||"))
            stats[term_id]["depth"] = min(path_depths) - 1  # Ensure topic is depth 0 = Root

    return stats


def _create_topics_dataframe(topic_stats: dict, collection_folder_name: str, cutoff_value: int, min_depth: int) -> \
        Tuple[pd.DataFrame, pd.DataFrame]:
    """
    Create the dataframe for the topics.

    :param topic_stats: The topic statistics.
    :param collection_folder_name: The folder name for the collection.
    :param cutoff_value: The cut-off for a term to be accepted.
    :param min_depth: The minimum depth to use.
    :return: The data frame with the topic statistics. One for the total and one for the reduced.
    """

    def transform_dataframe(dataframe: pd.DataFrame):
        """
        Transform the dataframe by combining columns, adding labels, and sorting the values according to the term depth.

        :param dataframe: The data frame.
        :return: The transformed data frame.
        """
        # Combine columns
        dataframe = dataframe.melt(id_vars=["Term", "Term ID", "Depth"], value_vars=["Strict", "Total"],
                                   var_name="Count Type", value_name="Count")
        # Create the label for the figure
        dataframe["Label"] = pd.Series(
            [f"{term} ({depth})" for (term, depth) in zip(dataframe['Term'], dataframe['Depth'])])
        dataframe = dataframe.sort_values("Depth")
        return dataframe

    df: pd.DataFrame = pd.DataFrame.from_dict(topic_stats, orient="index")
    # Remove unused columns, rename columns and set term ID as column
    del df["strict_ids"]
    del df["total_ids"]
    df.columns = ["Term", "Depth", "Strict", "Total"]
    df["Term ID"] = df.index
    # Remove terms, which was not found in the index list
    df = df[df.Depth != -1]
    df = df.reset_index()

    df_reduced: pd.DataFrame = df[(df["Total"] >= cutoff_value) & (df["Depth"] >= min_depth)]

    df = transform_dataframe(dataframe=df)
    df_reduced = transform_dataframe(dataframe=df_reduced)

    # df.to_excel(f"Resources/{collection_folder_name}/TopicsDataframe.xlsx")
    # df_reduced.to_excel(f"Resources/{collection_folder_name}/TopicsDataframe_Reduced.xlsx")

    return df, df_reduced


def _create_topics_statistics_plot(topic_stats_df: pd.DataFrame, collection_name: str, min_cutoff: int = None,
                                   min_cutoff_unit: str = None, min_depth: int = None):
    """
    Create statistics plot for the EDAM topics.

    :param topic_stats_df: The data frame with the statistics.
    :param collection_name: The collection name.
    :param min_cutoff: The minimum cutoff value. Default None.
    :param min_cutoff_unit: The minimum cutoff value unit. Default None.
    :param min_depth: The minimum depth. Default None.
    """
    g = sns.catplot(data=topic_stats_df, x="Label", y="Count", hue="Count Type", ci=None, kind="bar", orient="v",
                    legend=False)
    plt.legend(loc='upper right')
    g.axes[0, 0].set_xlabel("Term and depth")
    condition: str = ""
    if min_cutoff is not None:
        condition += f"Minimum {min_cutoff} {min_cutoff_unit} terms"
    if min_cutoff is not None and min_depth is not None:
        condition += " and "
    if min_depth is not None:
        condition += f"Minimum depth {min_depth}"
    condition = f"({condition}) "
    g.fig.suptitle(f"Terms {condition if len(condition) > 3 else ''}for the {collection_name} collection")
    g.set_xticklabels(rotation=90)
    plt.subplots_adjust(left=0.04, bottom=0.57, right=0.99, top=0.95)
    plt.show()


def _create_partial_topics_statistics_plot(topic_stats_df: pd.DataFrame, count_type: str, collection_name: str,
                                           min_cutoff: int = None, min_cutoff_unit: str = None, min_depth: int = None
                                           ):
    """
    Create partial statistics plot for the EDAM topics for one count type only

    :param topic_stats_df: The data frame with the statistics.
    :param collection_name: The collection name.
    :param min_cutoff: The minimum cutoff value. Default None.
    :param min_cutoff_unit: The minimum cutoff value unit. Default None.
    :param min_depth: The minimum depth. Default None.
    """
    topic_stats_df = topic_stats_df[topic_stats_df["Count Type"] == count_type]
    g = sns.catplot(data=topic_stats_df, x="Label", y="Count", hue="Count Type", ci=None, kind="bar", orient="v",
                    legend=False)
    g.axes[0, 0].set_xlabel("Term and depth")
    condition: str = ""
    if min_cutoff is not None:
        condition += f"Minimum {min_cutoff} {min_cutoff_unit} terms"
    if min_cutoff is not None and min_depth is not None:
        condition += " and "
    if min_depth is not None:
        condition += f"Minimum depth {min_depth}"
    condition = f"({condition}) "
    g.fig.suptitle(f"Terms {condition if len(condition) > 3 else ''}for the {collection_name} collection with only "
                   f"{count_type.lower()} terms")
    g.set_xticklabels(rotation=90)
    plt.subplots_adjust(left=0.04, bottom=0.51, right=0.99, top=0.95)
    plt.show()


class SetEncoder(json.JSONEncoder):
    def default(self, obj):
        if isinstance(obj, set):
            return list(obj)
        return json.JSONEncoder.default(self, obj)


if __name__ == "__main__":
    main()
