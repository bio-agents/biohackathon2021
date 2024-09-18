"""
Functions to calculate different time-dependent statistics.

"""

from collections import defaultdict
import datetime
import pandas as pd


def calculate_total_entries_over_time(agents: list):
    """
    Calculate the total entries over time.

    :param agents: The raw list of agents.
    :return: The statistics dictionary.
    """

    agent_time: dict = {agent["bioagentsID"]: datetime.date.fromisoformat(agent["additionDate"].split("T")[0])
                       for agent in agents}

    agent_time_df: pd.DataFrame = pd.DataFrame.from_dict(data=agent_time, orient="index")
    agent_time_df = agent_time_df.reset_index()
    agent_time_df.columns = ["ID", "AdditionDate"]

    stats_dict: defaultdict = defaultdict(lambda: {"count": 0})

    for date in _daterange(start_date=min(agent_time_df["AdditionDate"]), end_date=max(agent_time_df["AdditionDate"])):
        stats_dict[date] = len(agent_time_df[(agent_time_df["AdditionDate"] <= date)])

    return stats_dict


def _daterange(start_date, end_date):
    """
    Helper methods for iterating through the dates (Modified from https://stackoverflow.com/a/1060330).

    :param start_date: The start date.
    :param end_date: The end date (modified to be inclusive instead of exclusive).
    :return: The generator.
    """
    for n in range(int((end_date - start_date).days) + 1):
        yield start_date + datetime.timedelta(n)
