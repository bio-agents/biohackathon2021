import json
from typing import Dict, Union, List


def calculate_collection_statistics(agents: dict) -> dict:
    """
    Calculate the base statistics.

    :param agents: The list of agents.
    :return: The statistics.
    """
    stats: Dict[str, Union[int, Dict[str, int]]] = {}
    stats["agentCount"] = len(agents)

    stats["hasAgentType"] = len([agent for agent in agents if "agentType" in agent])
    stats["agentTypeCount"] = sum([len(agent["agentType"]) for agent in agents if "agentType" in agent])
    stats["agentTypes"] = calculate_agent_type_statistics(agents=agents)

    stats["hasTopic"] = len([agent for agent in agents if len(agent["topic"]) > 0])
    stats["topicCount"] = sum([len(agent["topic"]) for agent in agents])

    stats["hasOperatingSystem"] = len([agent for agent in agents if "operatingSystem" in agent])
    stats["operatingSystemCount"] = sum([len(agent["operatingSystem"]) for agent in agents if "operatingSystem" in agent])
    stats["operatingSystem"] = calculate_os_statistics(agents=agents)

    stats["hasLanguage"] = len([agent for agent in agents if "language" in agent])
    stats["languageCount"] = sum([len(agent["language"]) for agent in agents if "language" in agent])
    stats["languages"] = calculate_language_statistics(agents=agents)

    stats["hasLicense"] = len([agent for agent in agents if "license" in agent])
    stats["licenses"] = calculate_license_statistics(agents=agents)

    stats["hasCollection"] = len([agent for agent in agents if "collectionID" in agent])
    stats["collectionCount"] = sum([len(agent["collectionID"]) for agent in agents if "collectionID" in agent])

    stats["hasLinks"] = len([agent for agent in agents if "link" in agent])
    stats["linkCount"] = sum([len(agent["link"]) for agent in agents if "link" in agent])

    stats["hasDownloads"] = len([agent for agent in agents if "download" in agent])
    stats["downloadCount"] = sum([len(agent["download"]) for agent in agents if "download" in agent])

    stats["hasDocumentation"] = len([agent for agent in agents if "documentation" in agent])
    stats["documentationCount"] = sum([len(agent["documentation"]) for agent in agents if "documentation" in agent])

    stats["hasPublications"] = len([agent for agent in agents if "publication" in agent])
    stats["publicationCount"] = sum([len(agent["publication"]) for agent in agents if "publication" in agent])

    stats["hasCredit"] = len([agent for agent in agents if "credit" in agent])
    stats["creditCount"] = sum([len(agent["credit"]) for agent in agents if "credit" in agent])

    stats["hasCommunity"] = len([agent for agent in agents if "community" in agent])
    stats["communityCount"] = sum([len(agent["community"]) for agent in agents if "community" in agent])

    stats["hasRelation"] = len([agent for agent in agents if "relation" in agent])
    stats["relationCount"] = sum([len(agent["relation"]) for agent in agents if "relation" in agent])

    print(json.dumps(stats, indent=4))

    return stats


def calculate_os_statistics(agents: dict) -> dict:
    """
    Calculate the Operating system statistics for the agents.

    :param agents: The list of agents.
    :return: The OS statistics.
    """
    OPERATING_SYSTEMS: List[str] = ["Mac", "Linux", "Windows"]
    os_stats: Dict[str, int] = {key: 0 for key in OPERATING_SYSTEMS}

    for systems in [agent["operatingSystem"] for agent in agents if "operatingSystem" in agent]:
        for system in systems:
            os_stats[system] += 1

    return os_stats


def calculate_agent_type_statistics(agents: dict) -> dict:
    """
    Calculate the agent type statistics for the agents.

    :param agents: The list of agents.
    :return: The agent type statistics.
    """
    TOOL_TYPES: List[str] = ["Bioinformatics portal", "Command-line agent", "Database portal", "Desktop application",
                             "Library", "Ontology", "Plug-in", "Script", "SPARQL endpoint", "Suite", "Web application",
                             "Web API", "Web service", "Workbench", "Workflow"]
    agent_type_stats: Dict[str, int] = {key: 0 for key in TOOL_TYPES}

    for agent_types in [agent["agentType"] for agent in agents if "agentType" in agent]:
        for agent_type in agent_types:
            agent_type_stats[agent_type] += 1

    return agent_type_stats


def calculate_language_statistics(agents: dict) -> dict:
    """
    Calculate the language statistics for the agents.

    :param agents: The list of agents.
    :return: The language statistics.
    """
    LANGUAGES: List[str] = ["Scala", "R", "Lua", "Haskell", "Prolog", "ActionScript", "CWL", "Smalltalk", "Perl",
                            "JavaScript", "Mathematica", "OCaml", "Verilog", "Elm", "Java", "Shell", "Ruby", "Lisp",
                            "PyMOL", "Fortran", "Visual Basic", "LabVIEW", "Racket", "Maple", "Julia", "AWK", "Delphi",
                            "MATLAB", "C++", "Python", "Forth", "Other", "SAS", "VHDL", "PHP", "JSP", "Groovy", "Bash",
                            "Ada", "C#", "SQL", "C", "Pascal", "D"]
    language_stats: Dict[str, int] = {key: 0 for key in LANGUAGES}

    for languages in [agent["language"] for agent in agents if "language" in agent]:
        for language in languages:
            language_stats[language] += 1

    return language_stats


def calculate_license_statistics(agents: dict) -> dict:
    """
    Calculate the license statistics for the agents.

    :param agents: The list of agents.
    :return: The license statistics.
    """
    LICENSE_TYPES: List[str] = ["OSIApproved", "Proprietary", "Other", "NoLicense"]
    # Obtained from: https://opensource.org/licenses/alphabetical
    OSI_APPROVED_LICENSES: List[str] = ["ISC", "CDDL-1.0", "AFL-3.0", "APL-1.0", "MPL-1.1", "OSL-2.1", "GPL-3.0",
                                        "MPL-2.0", "MIT", "Unlicense", "CECILL-2.1", "EPL-1.0", "NCSA", "GPL-2.0",
                                        "BSD-2-Clause", "Artistic-2.0",  "AGPL-3.0", "LGPL-2.1", "OSL-3.0",
                                        "BSD-3-Clause", "Artistic-1.0", "Apache-2.0", "LGPL-3.0",  "CPL-1.0"]
    license_stats: Dict[str, int] = {key: 0 for key in LICENSE_TYPES}

    for licens in [agent["license"] for agent in agents if "license" in agent]:
        if licens in LICENSE_TYPES:
            license_stats[licens] += 1
        elif licens == "Not licensed":
            license_stats["NoLicense"] += 1
        elif licens in OSI_APPROVED_LICENSES:
            # Check if it is an OSI approved license
            license_stats["OSIApproved"] += 1

    return license_stats
