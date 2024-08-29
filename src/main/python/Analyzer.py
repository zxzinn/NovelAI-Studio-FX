import re
from collections import Counter

def load_bpe_vocab(file_path):
    vocab = []
    with open(file_path, 'r', encoding='utf-8') as file:
        next(file)  # 跳過版本行
        for line in file:
            vocab.append(line.strip())
    return vocab

def analyze_bpe_vocab(vocab):
    # 1. 詞彙表大小
    print(f"Vocabulary size: {len(vocab)}")

    # 2. 分類token
    full_words = [token for token in vocab if token.endswith('</w>')]
    subwords = [token for token in vocab if not token.endswith('</w>')]
    print(f"Full words: {len(full_words)}")
    print(f"Subwords: {len(subwords)}")

    # 3. 最常見的token
    print("Most common tokens:")
    print(Counter(vocab).most_common(10))

    # 4. 平均token長度
    avg_length = sum(len(token) for token in vocab) / len(vocab)
    print(f"Average token length: {avg_length:.2f}")

    # 5. 分析子詞結構
    prefixes = Counter(token[:2] for token in subwords)
    suffixes = Counter(token[-2:] for token in subwords)
    print("Most common prefixes:")
    print(prefixes.most_common(5))
    print("Most common suffixes:")
    print(suffixes.most_common(5))

    # 6. 特殊字符分析
    special_chars = re.findall(r'[^a-zA-Z0-9\s]', ' '.join(vocab))
    print("Special characters:")
    print(Counter(special_chars).most_common())

    # 7. 大小寫分析
    lowercase = sum(1 for token in vocab if token.islower())
    uppercase = sum(1 for token in vocab if token.isupper())
    mixed_case = len(vocab) - lowercase - uppercase
    print(f"Lowercase tokens: {lowercase}")
    print(f"Uppercase tokens: {uppercase}")
    print(f"Mixed case tokens: {mixed_case}")

# 使用函數
vocab = load_bpe_vocab('C:\\Users\\User\\Desktop\\bpe_simple_vocab_16e6.txt')
analyze_bpe_vocab(vocab)