import sys, requests

if len(sys.argv) == 2:
    base_url = sys.argv[1]
    answer = requests.get(base_url + '/start').json()['answer']
    solved = False
    guesses = 0
    while not solved:
        guess = input('Guess a card (or type "reveal"): ')
        if guess == 'reveal':
            print(answer)
            solved = True
        else:
            r = requests.get(base_url + '/guess', params={'guess': guess, 'answer': answer})
            if r.status_code == 200:
                guesses += 1
                json = r.json()
                solved = json['correct']
                print(json['message'])
            else:
                print(r.text)
            print()
    print('Solved in %d guesses.' % guesses)
else:
    print("Please provide Cardle server URL as a command-line argument")