def send_message(minescript, message):
    if message:
        minescript.show_chat_screen()
        minescript.set_chat_input(message)