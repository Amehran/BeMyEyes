class MapService:
    def calculate_clock_direction(self, object_bearing_deg: float, user_heading: float) -> str:
        """
        Calculates the clock face direction of an object relative to the user's current heading.
        0 deg = 12 o'clock (Ahead)
        90 deg = 3 o'clock (Right)
        """
        # Normalize to 0-360
        relative_angle = (object_bearing_deg - user_heading) % 360
        
        if relative_angle > 180:
            relative_angle -= 360 # Convert to -180 to +180 range
            
        # Mapping logic
        # Ahead: -22.5 to +22.5
        if -22.5 <= relative_angle < 22.5:
            return "12 o'clock"
        elif 22.5 <= relative_angle < 67.5:
            return "1-2 o'clock" # Diag Right
        elif 67.5 <= relative_angle < 112.5:
            return "3 o'clock" # Right
        elif 112.5 <= relative_angle < 157.5:
            return "4-5 o'clock"
        elif relative_angle >= 157.5 or relative_angle < -157.5:
            return "6 o'clock" # Behind
        elif -157.5 <= relative_angle < -112.5:
            return "7-8 o'clock"
        elif -112.5 <= relative_angle < -67.5:
            return "9 o'clock" # Left
        elif -67.5 <= relative_angle < -22.5:
            return "10-11 o'clock"
            
        return "Unknown"

map_service = MapService()
