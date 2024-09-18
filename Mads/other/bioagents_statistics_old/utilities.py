"""
Different utilities and helper methods.

"""
from boltons.iterutils import remap


def clean_list(raw_agents: list) -> list:
    """
    Clean the list of agents.

    :param raw_agents: The raw list of agents.
    :return: The cleaned list of agents.
    """
    # Clean the list
    drop_false = lambda path, key, value: bool(value)
    return remap(raw_agents, visit=drop_false)
