#! /bin/bash

osascript <<'EOF'
repeat 40 times
  tell application "System Events"
    repeat with p in (every process whose visible is true)
      try
        if exists (button "Allow" of window 1 of p) then
          click button "Allow" of window 1 of p
          return
        end if
        if exists (button "Allow" of sheet 1 of window 1 of p) then
          click button "Allow" of sheet 1 of window 1 of p
          return
        end if
      end try
    end repeat
  end tell
  delay 0.25
end repeat
EOF
