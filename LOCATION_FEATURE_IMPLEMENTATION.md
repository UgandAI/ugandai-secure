# Location Field Implementation

## Overview
Added a location field to the user registration process with three predefined choices: **Buyanga**, **Namutumba**, and **Mbale**.

## Changes Made

### 1. Android Application

#### UI Changes ([activity_signup.xml](app/src/main/res/layout/activity_signup.xml))
- Added a Spinner (dropdown) control between the password confirmation field and the signup button
- Spinner ID: `signup_location`
- Pre-populated with: "Select Location", "Buyanga", "Namutumba", "Mbale"

#### Java Logic ([SignupActivity.java](app/src/main/java/com/donatienthorez/ugandai/chat/ui/SignupActivity.java))
- Added `selectedLocation` field to track user selection
- Imported `AdapterView` and `ArrayAdapter` for spinner functionality
- Updated `performNetworkOperationUserRegisterAsync()` to accept location parameter
- Updated `userRegister()` to include location in JSON payload
- Added spinner initialization in `onCreate()` with item selection listener
- Updated validation to require location selection
- Modified JSON request body: `{"username": "%s", "password": "%s", "location": "%s"}`

### 2. Backend (FastAPI Server)

#### Database Model ([models.py](Web-Server-main/models.py))
- Added `location` column to `User` table:
  ```python
  location = _sql.Column(_sql.String, nullable=True)
  ```

#### Schema ([schemas.py](Web-Server-main/schemas.py))
- Added `location: Optional[str] = None` to `_UserBase` class
- Imported `Optional` from typing module
- This makes location optional for backward compatibility

#### Service Layer ([services.py](Web-Server-main/services.py))
- Updated `create_user()` to save location:
  ```python
  db_user = _models.User(
      username=user.username, 
      password_hash=bcrypt.hash(user.password),
      location=user.location
  )
  ```

#### API Endpoint ([main.py](Web-Server-main/main.py))
- Added location validation before user creation:
  ```python
  valid_locations = ["Buyanga", "Namutumba", "Mbale"]
  if user.location and user.location not in valid_locations:
      raise HTTPException(
          status_code=400, 
          detail=f"Invalid location. Must be one of: {', '.join(valid_locations)}"
      )
  ```

## Database Migration

### Important Notes:

1. **SQLite Automatic Schema Update**: The location column will be automatically added when the server restarts because SQLAlchemy's `create_database()` uses `metadata.create_all()`.

2. **Existing Data**: If you have existing users in `database.db`, they will have `NULL` for the location field (which is why we made it nullable).

3. **For Production**: If you need to migrate existing data, you should:
   ```python
   # Option 1: Update existing records manually
   UPDATE users SET location = 'Mbale' WHERE location IS NULL;
   
   # Option 2: Use Alembic for proper migrations
   ```

4. **To start fresh** (development only):
   ```bash
   cd Web-Server-main
   rm database.db  # Delete existing database
   python main.py  # Restart server - new schema will be created
   ```

## Testing the Implementation

### Android App Testing:
1. Open SignupActivity
2. Verify the location dropdown appears
3. Try submitting without selecting a location → Should show "All fields are mandatory"
4. Select a location and complete registration
5. Verify the user is created successfully

### Backend Testing:
```bash
# Test with valid location
curl -X POST "http://localhost:8000/users/register" \
  -H "Content-Type: application/json" \
  -d '{"username": "testuser", "password": "testpass", "location": "Mbale"}'

# Test with invalid location
curl -X POST "http://localhost:8000/users/register" \
  -H "Content-Type: application/json" \
  -d '{"username": "testuser", "password": "testpass", "location": "Kampala"}'
# Expected: 400 error with "Invalid location" message

# Test without location (should work due to Optional)
curl -X POST "http://localhost:8000/users/register" \
  -H "Content-Type: application/json" \
  -d '{"username": "testuser", "password": "testpass"}'
```

## Security Considerations

1. **Client-side validation**: The Android app requires location selection
2. **Server-side validation**: The backend validates location against allowed values
3. **SQL Injection**: Protected by SQLAlchemy ORM parameterized queries
4. **Backward compatibility**: Location is optional, so old API calls still work

## Future Enhancements

Consider these improvements:

1. **Dynamic Location List**: Store locations in database instead of hardcoding
2. **Location-based Content**: Use location to personalize farming advice
3. **User Profile**: Add location to user profile for editing after signup
4. **Analytics**: Track user distribution by location
5. **Weather Integration**: Use location to provide region-specific weather data (already mentioned in README with buyangaWeather.json, mbaleWeather.json, namutumbaWeather.json)

## Related Files

### Android:
- [SignupActivity.java](app/src/main/java/com/donatienthorez/ugandai/chat/ui/SignupActivity.java)
- [activity_signup.xml](app/src/main/res/layout/activity_signup.xml)

### Backend:
- [models.py](Web-Server-main/models.py)
- [schemas.py](Web-Server-main/schemas.py)
- [services.py](Web-Server-main/services.py)
- [main.py](Web-Server-main/main.py)
- [database.py](Web-Server-main/database.py)

## Deployment Checklist

- [ ] Test signup with all three locations
- [ ] Verify location is stored in database
- [ ] Test with verified users (Bob, John, Aran, Yirga, Brad)
- [ ] Backup existing database before deploying
- [ ] Update environment variables if needed
- [ ] Test login still works for existing users
- [ ] Verify location data in OpenAI context (if using it for personalized responses)
