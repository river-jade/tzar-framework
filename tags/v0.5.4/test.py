L = [1,2,3,4]

size = 2

test = []
M = L[:]


for index in range(len(M)):
    test( + [M[index]])
    M = L[0:index] + L[index+1:]

size: 1
[1], [2], [3], [4],
size: 2
[1,2], [1,3], [1,4], [2,3], [2,4], [3,4]
size: 3
[1,2,3],[1,2,4],[1,3,4], [2,3,4]


stack = [], [], [], [], [1], [1], [1], [2], [2], [3], [1,2], [1,2], [1,3], [2,3]


L = [1,2,3,4]
for n in range(1, len(L)):
    test_n_sets(L, n)

def test_n_sets(L, M, n):
    if (n == 0):
        test(L + M)
    for index in range(1, len(L)):
        test_n_sets(L[0:index] + L[index+1:], M + [L[index]], n - 1)

def test(sublist):
    print sublist