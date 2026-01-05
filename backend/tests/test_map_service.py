import pytest
from app.services.mapping import MapService

def test_relative_bearing_calculation():
    service = MapService()
    
    # Object is due North (0 degrees)
    object_bearing = 0.0
    
    # Case 1: User faces North (0) -> Object is straight ahead (12 o'clock)
    clock_dir = service.calculate_clock_direction(object_bearing, user_heading=0.0)
    assert clock_dir == "12 o'clock"

    # Case 2: User faces East (90) -> Object is to the Left (9 o'clock)
    clock_dir = service.calculate_clock_direction(object_bearing, user_heading=90.0)
    assert clock_dir == "9 o'clock"

    # Case 3: User faces South (180) -> Object is Behind (6 o'clock)
    clock_dir = service.calculate_clock_direction(object_bearing, user_heading=180.0)
    assert clock_dir == "6 o'clock"

    # Case 4: User faces West (270) -> Object is Right (3 o'clock)
    clock_dir = service.calculate_clock_direction(object_bearing, user_heading=270.0)
    assert clock_dir == "3 o'clock"
