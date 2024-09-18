"""
The script for calculating the different statistics for a given agent list.
"""
from datetime import datetime
from typing import Dict, Union, List

from ._utilities import clean_and_filter_agent_list
from ._spdx_license_parser import parse_license_list, LicensesData


def calculate_general_statistics(agents: list, upper_time_limit: datetime = datetime.today()):
    """
    Calculate the general statistics for a list of agents.

    :param agents: The list of agents.
    :param upper_time_limit: Calculate the statistics for agents added up to the time limit.
        Default: datetime.datetime.today()
    :return: The dictionary with the statistics.
    """
    # Clean the list of agents
    agents = clean_and_filter_agent_list(raw_agents=agents, upper_time_limit=upper_time_limit)

    # Create the dictionary to hold the statistics and calculate the statistics
    stats: Dict[str, Union[str, int, Dict[str, int]]] = {}
    stats["date"] = upper_time_limit.isoformat(timespec="seconds")
    stats["agentCount"] = len(agents)

    stats["hasAgentType"] = len([agent for agent in agents if "agentType" in agent])
    stats["agentTypeCount"] = sum([len(agent["agentType"]) for agent in agents if "agentType" in agent])
    stats["agentTypes"] = _calculate_agent_type_statistics(agents=agents)

    stats["hasTopic"] = len([agent for agent in agents if "topic" in agent])
    stats["topicCount"] = sum([len(agent["topic"]) for agent in agents if "topic" in agent])

    stats["hasOperatingSystem"] = len([agent for agent in agents if "operatingSystem" in agent])
    stats["operatingSystemCount"] = sum([len(agent["operatingSystem"]) for agent in agents if "operatingSystem" in agent])
    stats["operatingSystem"] = _calculate_os_statistics(agents=agents)

    stats["hasLanguage"] = len([agent for agent in agents if "language" in agent])
    stats["languageCount"] = sum([len(agent["language"]) for agent in agents if "language" in agent])
    stats["languages"] = _calculate_language_statistics(agents=agents)

    stats["hasLicense"] = len([agent for agent in agents if "license" in agent])
    stats["licenses"] = _calculate_license_statistics(agents=agents)

    stats["hasMaturity"] = len([agent for agent in agents if "maturity" in agent])
    stats["maturity"] = _calculate_maturity_statistics(agents=agents)

    stats["hasCost"] = len([agent for agent in agents if "cost" in agent])
    stats["costs"] = _calculate_cost_statistics(agents=agents)

    stats["hasCollection"] = len([agent for agent in agents if "collectionID" in agent])
    stats["collectionCount"] = sum([len(agent["collectionID"]) for agent in agents if "collectionID" in agent])

    stats["hasCodeAccessibility"] = len([agent for agent in agents if "accessibility" in agent])
    stats["accessibility"] = _calculate_code_accessibility_statistics(agents=agents)

    stats["hasiEchorPlatform"] = len([agent for agent in agents if "iechorPlatform" in agent])
    stats["iechorPlatformCount"] = sum([len(agent["iechorPlatform"]) for agent in agents if "iechorPlatform" in agent])
    stats["iechorPlatform"] = _calculate_iechor_platform_statistics(agents=agents)

    stats["hasiEchorNode"] = len([agent for agent in agents if "iechorNode" in agent])
    stats["iechorNodeCount"] = sum([len(agent["iechorNode"]) for agent in agents if "iechorNode" in agent])
    stats["iechorNodes"] = _calculate_iechor_node_statistics(agents=agents)

    stats["hasiEchorCommunity"] = len([agent for agent in agents if "iechorCommunity" in agent])
    stats["iechorCommunityCount"] = sum([len(agent["iechorCommunity"]) for agent in agents if "iechorCommunity" in agent])
    stats["iechorCommunity"] = _calculate_iechor_community_statistics(agents=agents)

    stats["hasLinks"] = len([agent for agent in agents if "link" in agent])
    stats["linkCount"] = sum([len(agent["link"]) for agent in agents if "link" in agent])
    stats["linkTypes"] = _calculate_link_type_statistics(agents=agents)

    stats["hasDownloads"] = len([agent for agent in agents if "download" in agent])
    stats["downloadCount"] = sum([len(agent["download"]) for agent in agents if "download" in agent])
    stats["downloadTypes"] = _calculate_download_type_statistics(agents=agents)

    stats["hasDocumentation"] = len([agent for agent in agents if "documentation" in agent])
    stats["documentationCount"] = sum([len(agent["documentation"]) for agent in agents if "documentation" in agent])
    stats["documentationTypes"] = _calculate_documentation_type_statistics(agents=agents)

    stats["hasPublications"] = len([agent for agent in agents if "publication" in agent])
    stats["publicationCount"] = sum([len(agent["publication"]) for agent in agents if "publication" in agent])
    stats["publicationTypes"] = _calculate_publication_type_statistics(agents=agents)

    stats["hasCredit"] = len([agent for agent in agents if "credit" in agent])
    stats["hasCreditRole"] = len([agent for agent in agents if ("credit" in agent and
                                                             len([role for role in agent["credit"] if
                                                                  "credit" in agent]) > 0)])
    stats["creditCount"] = sum([len(agent["credit"]) for agent in agents if "credit" in agent])
    stats["creditRoleTypes"] = _calculate_credit_role_type_statistics(agents=agents)

    stats["hasRelation"] = len([agent for agent in agents if "relation" in agent])
    stats["relationCount"] = sum([len(agent["relation"]) for agent in agents if "relation" in agent])
    stats["relations"] = _calculate_relation_statistics(agents=agents)

    stats["hasBiolib"] = len([agent for agent in agents if "community" in agent])
    stats["BiolibCount"] = sum([len(agent["community"]) for agent in agents if "community" in agent])

    return stats


def _calculate_agent_type_statistics(agents: list) -> dict:
    """
    Calculate the agent type statistics for the agents.

    :param agents: The list of agents.
    :return: The agent type statistics.
    """
    # TODO: Consider non-hardcoded approach
    TOOL_TYPES: List[str] = ["Bioinformatics portal", "Command-line agent", "Database portal", "Desktop application",
                             "Library", "Ontology", "Plug-in", "Script", "SPARQL endpoint", "Suite", "Web application",
                             "Web API", "Web service", "Workbench", "Workflow"]
    agent_type_stats: Dict[str, int] = {key: 0 for key in TOOL_TYPES}

    for agent_types in [agent["agentType"] for agent in agents if "agentType" in agent]:
        for agent_type in agent_types:
            agent_type_stats[agent_type] += 1

    return agent_type_stats


def _calculate_os_statistics(agents: list) -> dict:
    """
    Calculate the Operating system statistics for the agents.

    :param agents: The list of agents.
    :return: The OS statistics.
    """
    # TODO: Consider non-hardcoded approach
    OPERATING_SYSTEMS: List[str] = ["Mac", "Linux", "Windows"]
    os_stats: Dict[str, int] = {key: 0 for key in OPERATING_SYSTEMS}

    for systems in [agent["operatingSystem"] for agent in agents if "operatingSystem" in agent]:
        for system in systems:
            os_stats[system] += 1

    return os_stats


def _calculate_language_statistics(agents: list) -> dict:
    """
    Calculate the language statistics for the agents.

    :param agents: The list of agents.
    :return: The language statistics.
    """
    # TODO: Consider non-hardcoded approach
    LANGUAGES: List[str] = ["ActionScript", "Ada", "AppleScript", "Assembly language", "AWK", "Bash", "C", "C#", "C++",
                            "COBOL", "ColdFusion", "CWL", "D", "Delphi", "Dylan", "Eiffel", "Elm", "Forth", "Fortran",
                            "Groovy", "Haskell", "Icarus", "Java", "JavaScript", "JSP", "Julia", "LabVIEW", "Lisp",
                            "Lua", "Maple", "Mathematica", "MATLAB", "MLXTRAN", "NMTRAN", "OCaml", "Pascal", "Perl",
                            "PHP", "Prolog", "PyMOL", "Python", "R", "Racket", "REXX", "Ruby", "SAS", "Scala", "Scheme",
                            "Shell", "Smalltalk", "SQL", "Turing", "Verilog", "VHDL", "Visual Basic", "XAML", "Other"]
    language_stats: Dict[str, int] = {key: 0 for key in LANGUAGES}

    for languages in [agent["language"] for agent in agents if "language" in agent]:
        for language in languages:
            language_stats[language] += 1

    return language_stats


def _calculate_license_statistics(agents: list) -> dict:
    """
    Calculate the license statistics for the agents.

    :param agents: The list of agents.
    :return: The license statistics.
    """
    license_info: LicensesData = parse_license_list()

    LICENSE_TYPES: List[str] = ["OSIApproved", "FSFApproved", "Freeware", "Proprietary", "Other", "NoLicense",
                                "DeprecatedIdentifier"] + license_info.licenses_list
    OSI_APPROVED_LICENSES: List[str] = license_info.osi_approved_licenses
    FSF_APPROVED_LICENSES: List[str] = license_info.fsf_approved_licenses
    DEPRECATED_LICENSE_IDENTIFIERS: List[str] = license_info.deprecated_licenses

    license_stats: Dict[str, int] = {key: 0 for key in LICENSE_TYPES}

    for licens in [agent["license"] for agent in agents if "license" in agent]:
        if licens in LICENSE_TYPES:
            license_stats[licens] += 1
        elif licens == "Not licensed":
            license_stats["NoLicense"] += 1
        if licens in OSI_APPROVED_LICENSES:
            # Check if it is an OSI approved license
            license_stats["OSIApproved"] += 1
        if licens in FSF_APPROVED_LICENSES:
            # Check if it is an FSF approved license
            license_stats["FSFApproved"] += 1
        if licens in DEPRECATED_LICENSE_IDENTIFIERS:
            # Check if the license has a deprecated license identifier
            license_stats["DeprecatedIdentifier"] += 1
    return license_stats


def _calculate_maturity_statistics(agents: list) -> dict:
    """
    Calculate the code accessibility statistics for the agents.

    :param agents: The list of agents.
    :return: The code accessibility language statistics.
    """
    # TODO: Consider non-hardcoded approach
    MATURITY: List[str] = ["Emerging", "Mature", "Legacy"]
    maturity_stats: Dict[str, int] = {key: 0 for key in MATURITY}

    for maturity in [agent["maturity"] for agent in agents if "maturity" in agent]:
        maturity_stats[maturity] += 1

    return maturity_stats


def _calculate_cost_statistics(agents: list) -> dict:
    """
    Calculate the cost statistics for the agents.

    :param agents: The list of agents.
    :return: The cost statistics.
    """
    # TODO: Consider non-hardcoded approach
    COSTS: List[str] = ["Free of charge", "Free of charge (with restrictions)", "Commercial"]
    cost_stats: Dict[str, int] = {key: 0 for key in COSTS}

    for cost in [agent["cost"] for agent in agents if "cost" in agent]:
        cost_stats[cost] += 1

    return cost_stats


def _calculate_code_accessibility_statistics(agents: list) -> dict:
    """
    Calculate the code accessibility statistics for the agents.

    :param agents: The list of agents.
    :return: The code accessibility language statistics.
    """
    # TODO: Consider non-hardcoded approach
    ACCESSIBILITY: List[str] = ["Restricted access", "Open access", "Open access (with restrictions)"]
    accessibility_stats: Dict[str, int] = {key: 0 for key in ACCESSIBILITY}

    for accessibility in [agent["accessibility"] for agent in agents if "accessibility" in agent]:
        accessibility_stats[accessibility] += 1

    return accessibility_stats


def _calculate_iechor_platform_statistics(agents: list) -> dict:
    """
    Calculate the IECHOR platform statistics for the agents.

    :param agents: The list of agents.
    :return: The IECHOR platform statistics.
    """
    # TODO: Consider non-hardcoded approach
    PLATFORMS: List[str] = ["Data", "Agents", "Compute", "Interoperability", "Training"]
    platform_stats: Dict[str, int] = {key: 0 for key in PLATFORMS}

    for platforms in [agent["iechorPlatform"] for agent in agents if "iechorPlatform" in agent]:
        for platform in platforms:
            platform_stats[platform] += 1

    return platform_stats


def _calculate_iechor_node_statistics(agents: list) -> dict:
    """
    Calculate the IECHOR node statistics for the agents.

    :param agents: The list of agents.
    :return: The IECHOR node statistics.
    """
    # TODO: Consider non-hardcoded approach
    NODES: List[str] = ["Belgium", "Czech Republic", "Denmark", "EMBL", "Estonia", "Finland", "France", "Germany",
                        "Greece", "Hungary", "Ireland", "Israel", "Italy", "Luxembourg", "Netherlands", "Norway",
                        "Portugal", "Slovenia", "Spain", "Sweden", "Switzerland", "UK"]
    node_stats: Dict[str, int] = {key: 0 for key in NODES}

    for nodes in [agent["iechorNode"] for agent in agents if "iechorNode" in agent]:
        for node in nodes:
            node_stats[node] += 1

    return node_stats


def _calculate_iechor_community_statistics(agents: list) -> dict:
    """
    Calculate the IECHOR community statistics for the agents.

    :param agents: The list of agents.
    :return: The IECHOR community statistics.
    """
    # TODO: Consider non-hardcoded approach
    COMMUNITY: List[str] = ["3D-BioInfo", "Federated Human Data", "Galaxy", "Human Copy Number Variation",
                            "Intrinsically Disordered Proteins", "Marine Metagenomics", "Metabolomics",
                            "Microbial Biotechnology", "Plant Sciences", "Proteomics", "Rare Diseases"]
    community_stats: Dict[str, int] = {key: 0 for key in COMMUNITY}

    for communities in [agent["iechorCommunity"] for agent in agents if "iechorCommunity" in agent]:
        for community in communities:
            community_stats[community] += 1

    return community_stats


def _calculate_link_type_statistics(agents: list) -> dict:
    """
    Calculate the link type statistics for the agents.

    :param agents: The list of agents.
    :return: The link type statistics.
    """
    # TODO: Consider non-hardcoded approach
    LINK_TYPES: List[str] = ["Discussion forum", "Galaxy service", "Helpdesk", "Issue tracker", "Mailing list",
                             "Mirror", "Software catalogue", "Repository", "Social media", "Service",
                             "Technical monitoring", "Other"]
    link_type_stats: Dict[str, int] = {key: 0 for key in LINK_TYPES}

    for links in [agent["link"] for agent in agents if "link" in agent]:
        for link in links:
            for link_type in [t for t in link["type"]]:
                link_type_stats[link_type] += 1

    return link_type_stats


def _calculate_download_type_statistics(agents: list) -> dict:
    """
    Calculate the download type statistics for the agents.

    :param agents: The list of agents.
    :return: The download type statistics.
    """
    # TODO: Consider non-hardcoded approach
    DOWNLOAD_TYPES: List[str] = ["API specification", "Biological data", "Binaries", "Command-line specification",
                                 "Container file", "Icon", "Screenshot", "Source code", "Software package", "Test data",
                                 "Test script", "Agent wrapper (CWL)", "Agent wrapper (Galaxy)", "Agent wrapper (Taverna)",
                                 "Agent wrapper (Other)", "VM image", "Downloads page", "Other"]
    download_type_stats: Dict[str, int] = {key: 0 for key in DOWNLOAD_TYPES}

    for downloads in [agent["download"] for agent in agents if "download" in agent]:
        for download in downloads:
            download_type_stats[download["type"]] += 1

    return download_type_stats


def _calculate_documentation_type_statistics(agents: list) -> dict:
    """
    Calculate the documentation type statistics for the agents.

    :param agents: The list of agents.
    :return: The documentation type statistics.
    """
    # TODO: Consider non-hardcoded approach
    DOCUMENTATION_TYPES: List[str] = ["API documentation", "Citation instructions", "Code of conduct",
                                      "Command-line options", "Contributions policy", "FAQ", "General",
                                      "Governance", "Installation instructions", "Quick start guide", "Release notes",
                                      "Terms of use", "Training material", "User manual", "Other"]
    documentation_type_stats: Dict[str, int] = {key: 0 for key in DOCUMENTATION_TYPES}

    for documentations in [agent["documentation"] for agent in agents if "documentation" in agent]:
        for documentation in documentations:
            for link_type in [t for t in documentation["type"]]:
                documentation_type_stats[link_type] += 1

    return documentation_type_stats


def _calculate_publication_type_statistics(agents: list) -> dict:
    """
    Calculate the publication type statistics for the agents.

    :param agents: The list of agents.
    :return: The publication type statistics.
    """
    # TODO: Consider non-hardcoded approach
    PUBLICATION_TYPES: List[str] = ["Primary", "Method", "Usage", "Benchmarking study", "Review", "Other"]
    publication_type_stats: Dict[str, int] = {key: 0 for key in PUBLICATION_TYPES}

    for publications in [agent["publication"] for agent in agents if "publication" in agent]:
        for publication in [pub for pub in publications if "type" in pub]:
            for pub_type in [t for t in publication["type"]]:
                publication_type_stats[pub_type] += 1

    return publication_type_stats


def _calculate_credit_role_type_statistics(agents: list) -> dict:
    """
    Calculate the credit role type statistics for the agents.

    :param agents: The list of agents.
    :return: The credit role type statistics.
    """
    # TODO: Consider non-hardcoded approach
    CREDIT_ROLE_TYPES: List[str] = ["Developer", "Maintainer", "Provider", "Documentor", "Contributor", "Support",
                                    "Primary contact"]
    credit_role_type_stats: Dict[str, int] = {key: 0 for key in CREDIT_ROLE_TYPES}

    for creds in [agent["credit"] for agent in agents if "credit" in agent]:
        for credit in creds:
            for credit_type in [t for t in credit["typeRole"] if "typeRole" in credit]:
                credit_role_type_stats[credit_type] += 1
    return credit_role_type_stats


def _calculate_relation_statistics(agents: list) -> dict:
    """
    Calculate the relation statistics for the agents.

    :param agents: The list of agents.
    :return: The relation statistics.
    """
    # TODO: Consider non-hardcoded approach
    RELATION_TYPES: List[str] = ["isNewVersionOf", "hasNewVersion", "uses", "usedBy", "includes", "includedIn"]
    relation_stats: Dict[str, int] = {key: 0 for key in RELATION_TYPES}

    for relations in [agent["relation"] for agent in agents if "relation" in agent]:
        for relation in relations:
            relation_stats[relation["type"]] += 1

    return relation_stats
