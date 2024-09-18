"""
Different utilities and helper methods.

"""
import datetime

import dateutil
from dateutil import parser
import pytz
from boltons.iterutils import remap


def clean_and_filter_agent_list(raw_agents: list, upper_time_limit: datetime.datetime) -> list:
    """
    Clean the list of agents.

    :param raw_agents: The raw list of agents.
    :param upper_time_limit: Calculate the statistics for agents added up to the time limit.
        Default: datetime.datetime.today()
    :return: The cleaned list of agents.
    """
    # Clean the list
    drop_false = lambda path, key, value: bool(value)
    unfiltered_agents = remap(raw_agents, visit=drop_false)
    # Filter the agents according to the upper time limit
    return [agent for agent in unfiltered_agents
            if dateutil.parser.isoparse(agent["additionDate"]) < pytz.utc.localize(upper_time_limit)]
