## Outline

+ Select username
+ Join --> Select seats, show seats, seat colors
+ pull + cache asset data

+ mouse cursor
+ Drag cards
- Group cards
+ flip cards
- shuffle cards
- Hand per player
- shared text field for all game.players, editable only by host


- touch?
- order may be different when same timestamp --> sort by id

enhancements:

options, sliders for card corner radius, line width, mipmapping for ff



stack:
- stackable references stack, is still contained in gameObjects
- is rendered -> is in stack (important for input)
- stack has list of elements, server sends the entire stack each time
- flip -> flip individual elements, inverse order 