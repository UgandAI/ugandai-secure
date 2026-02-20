from typing import List, Optional
import datetime as _dt
import pydantic as _pydantic


class _UserBase(_pydantic.BaseModel):
    username: str
    location: Optional[str] = None


class UserCreate(_UserBase):
    password: str


class User(_UserBase):
    id: int

    class Config:
        orm_mode = True