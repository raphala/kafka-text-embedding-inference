import numpy as np
import pandas as pd

from main import get_papers, PAGES
from transformers import AutoTokenizer


if __name__ == '__main__':
    tokenizer = AutoTokenizer.from_pretrained('sentence-transformers/all-MiniLM-L6-v2')
    current_cursor = "*"
    stats = {
        'title': {'words': [], 'chars': [], 'tokens': []},
        'abstract': {'words': [], 'chars': [], 'tokens': []}
    }
    for i in range(PAGES):
        current_cursor, papers = get_papers(current_cursor)
        for paper in papers:
            for section in ['title', 'abstract']:
                text = getattr(paper, section)
                stats[section]['words'].append(len(text.split()))
                stats[section]['chars'].append(len(text))
                stats[section]['tokens'].append(len(tokenizer.tokenize(text)))

    results = {}
    for section, data in stats.items():
        df = pd.DataFrame({
            'word_count': data['words'],
            'char_count': data['chars'],
            'token_count': data['tokens']
        })

        results[section] = df.describe()
        results[section]['word_median'] = np.median(data['words'])
        results[section]['char_median'] = np.median(data['chars'])
        results[section]['tokens_median'] = np.median(data['tokens'])

    print(results)


# 'title':  word_count    char_count   token_count  word_median  char_median  tokens_median
# count  10000.000000  10000.000000  10000.000000         14.0        109.0           22.0
# mean      14.397800    112.056500     24.200700         14.0        109.0           22.0
# std        5.104288     38.955203     11.940221         14.0        109.0           22.0
# min        1.000000      7.000000      2.000000         14.0        109.0           22.0
# 25%       11.000000     85.000000     17.000000         14.0        109.0           22.0
# 50%       14.000000    109.000000     22.000000         14.0        109.0           22.0
# 75%       17.000000    134.000000     29.000000         14.0        109.0           22.0
# max       84.000000    588.000000    203.000000         14.0        109.0           22.0,
#
# 'abstract':  word_count    char_count   token_count  word_median  char_median  tokens_median
# count  10000.000000  10000.000000  10000.000000        202.0       1478.0          313.0
# mean     192.224000   1377.277700    304.439100        202.0       1478.0          313.0
# std      102.661444    722.978091    174.170744        202.0       1478.0          313.0
# min        0.000000      0.000000      0.000000        202.0       1478.0          313.0
# 25%      148.000000   1065.750000    218.000000        202.0       1478.0          313.0
# 50%      202.000000   1478.000000    313.000000        202.0       1478.0          313.0
# 75%      250.000000   1807.000000    404.000000        202.0       1478.0          313.0
# max      916.000000   5961.000000   1246.000000        202.0       1478.0          313.0
