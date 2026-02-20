# Update from Partner A: Location Field Added to Signup

Hey Partner B,

I added a **location field** to the Android signup flow. Users now select their region: **Buyanga**, **Namutumba**, or **Mbale**.

## Android Changes (Already Done)
- **SignupActivity.java**: Added location spinner, sends location in registration request
- **activity_signup.xml**: Added Spinner UI element
- Registration JSON now includes: `{"username": "...", "password": "...", "location": "..."}`

## Backend Changes (You Need to Make)

The Android app is now sending location data, but the actual functioning server needs these updates:

### 1. **models.py** - Add location column
```python
class User(_database.Base):
    __tablename__ = "users"
    id = _sql.Column(_sql.Integer, primary_key=True, index=True)
    username = _sql.Column(_sql.String, unique=True, index=True)
    password_hash = _sql.Column(_sql.String)
    location = _sql.Column(_sql.String, nullable=True)  # ADD THIS LINE
```

### 2. **schemas.py** - Accept location in API
Add import:
```python
from typing import List, Optional  # Add Optional
```

Update schema:
```python
class _UserBase(_pydantic.BaseModel):
    username: str
    location: Optional[str] = None  # ADD THIS LINE
```

### 3. **services.py** - Save location to database
```python
def create_user(db: _orm.Session, user: _schemas.UserCreate):
    db_user = _models.User(
        username=user.username, 
        password_hash=bcrypt.hash(user.password),
        location=user.location  # ADD THIS LINE
    )
    db.add(db_user)
    db.commit()
    db.refresh(db_user)
    return db_user
```

### 4. **main.py** - Validate location values
In the `/users/register` endpoint, add validation after username verification:
```python
@app.post("/users/register", response_model=_schemas.User)
async def create_user(user: _schemas.UserCreate, db: _orm.Session = Depends(_services.get_db)):
    
    # ... existing verified users check ...
    
    # ADD THIS VALIDATION:
    valid_locations = ["Buyanga", "Namutumba", "Mbale"]
    if user.location and user.location not in valid_locations:
        raise HTTPException(
            status_code=400, 
            detail=f"Invalid location. Must be one of: {', '.join(valid_locations)}"
        )
    
    # ... rest of existing code ...
```

### 5. **Database Migration**
After making the code changes:

**Option A (Development - Fresh Start):**
```bash
rm database.db
# Restart server - new schema auto-created
```

**Option B (Production - Keep Existing Data):**
SQLAlchemy will add the column automatically, but existing users will have `NULL` for location. If needed, update them manually or leave as-is.

## How You Can Use This

Once implemented, you can access user location in chat endpoints:
```python
user = await get_token(token, db)
user_location = user.location  # "Buyanga", "Namutumba", "Mbale", or None
```

**Suggested Enhancement**: Pass location to OpenAI assistant for region-specific advice. Weather files already exist: buyangaWeather.json, mbaleWeather.json, namutumbaWeather.json.

Let me know when you push these changes so I can test end-to-end!

— Partner A
