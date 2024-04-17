file_path = '/Users/cc/CS440cc/pa1/output500h.txt'

# Initialize counters for each player's wins
wins_player_1 = 0
wins_player_2 = 0

# Open and read the file
with open(file_path, 'r') as file:
    for line in file:
        if "[INFO] Main.main: winner=player 1" in line:
            wins_player_1 += 1
        elif "[INFO] Main.main: winner=player 2" in line:
            wins_player_2 += 1

# Print the counts
print(f"Player 1 wins: {wins_player_1}")
print(f"Player 2 wins: {wins_player_2}")