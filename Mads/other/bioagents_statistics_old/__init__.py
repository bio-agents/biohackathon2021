"""
The package with the different functions for calculating different statistics in https://bio.agents.
"""

from .time_statistics import calculate_total_entries_over_time
from .edam_statistics import find_top_terms, get_edam_topics, get_edam_operation, get_edam_format, get_edam_data
from .base_statistics import calculate_collection_statistics
