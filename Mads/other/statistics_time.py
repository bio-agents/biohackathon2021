import json

from boltons.iterutils import remap
from matplotlib import pyplot as plt
import matplotlib.dates as mdates
import pandas as pd
import seaborn as sns

from bioagents_statistics import calculate_collection_statistics


def main():
    plt.style.use("ggplot")
    # Read the agents
    collection_name: str = ""
    with open(f"Resources/{collection_name.title().replace(' ', '')}Collection/Agents.json", "r", encoding="utf8") as f:
        raw_agents = json.load(f)

    drop_false = lambda path, key, value: bool(value)
    agents = remap(raw_agents, visit=drop_false)

    calculate_statistics(raw_agents=agents, collection_name=collection_name)

    stats_dict = calculate_collection_statistics(agents=agents)

def calculate_statistics(raw_agents: list, collection_name: str, ):
    """
        Calculate the statistics.

        :param raw_agents: The raw list of agents.
        :param collection_name: The name of the collection.
        :return:
        """
    # Clean the list


    #stats_dict: dict = calculate_total_entries_over_time(agents=agents)


    # _create_total_entries_plot(stats_dict=stats_dict, collection_name=collection_name)


def _create_total_entries_plot(stats_dict, collection_name):
    """

    :param stats_dict: The stats dictionary
    :param collection_name: The name of the collection.
    """
    stats_df: pd.DataFrame = pd.DataFrame.from_dict(stats_dict, orient="index", columns=["Count"])
    # Format the axis
    fig, ax = plt.subplots()
    ax = sns.lineplot(data=stats_df, x=stats_df.index, y="Count")
    ax.set_xlabel("Time")
    ax.xaxis.set_major_locator(mdates.MonthLocator(interval=2))  # Every 2 month
    ax.xaxis.set_major_formatter(mdates.DateFormatter('%Y-%m'))
    ax.set_title(f"The number of agents over time in the {collection_name} collection")
    ax.set_xlim(xmin=min(stats_df.index), xmax=max(stats_df.index))
    fig.autofmt_xdate()
    plt.show()


if __name__ == "__main__":
    main()
