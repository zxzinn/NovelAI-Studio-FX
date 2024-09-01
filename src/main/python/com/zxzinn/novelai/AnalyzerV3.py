import re
from collections import Counter
from typing import List, Tuple, Dict

def load_bpe_vocab(file_path: str) -> List[str]:
    with open(file_path, 'r', encoding='utf-8') as file:
        next(file)  # 跳過版本行
        return [line.strip() for line in file]

def extract_words(vocab: List[str]) -> Tuple[List[str], List[str]]:
    full_words = [token[:-4] for token in vocab if token.endswith('</w>')]
    subwords = [token for token in vocab if not token.endswith('</w>')]
    return full_words, subwords

def identify_weighted_words(vocab: List[str]) -> Dict[str, float]:
    weighted_words = {}

    for token in vocab:
        weight = 1.0

        # 完整詞可能有更高的權重
        if token.endswith('</w>'):
            weight *= 1.2
            token = token[:-4]  # 移除 '</w>' 標記

        # 包含特殊字符的詞可能有特殊用途
        if re.search(r'[^a-zA-Z0-9\s]', token):
            weight *= 1.1

        # 全大寫的詞可能是重要的縮寫或專有名詞
        if token.isupper() and len(token) > 1:
            weight *= 1.3

        # 非常長的詞可能是專業術語
        if len(token) > 15:
            weight *= 1.2

        # 非常短的詞（除了停用詞）可能不太重要
        if len(token) < 3 and token.lower() not in ['a', 'an', 'the', 'of', 'to', 'in', 'for', 'on', 'at', 'is', 'are']:
            weight *= 0.8

        weighted_words[token] = weight

    return weighted_words

def analyze_vocab(file_path: str):
    vocab = load_bpe_vocab(file_path)
    full_words, subwords = extract_words(vocab)
    weighted_words = identify_weighted_words(vocab)

    print(f"Total vocabulary size: {len(vocab)}")
    print(f"Number of full words: {len(full_words)}")
    print(f"Number of subwords: {len(subwords)}")

    print("\nSample of full words:")
    print(full_words[:20])

    print("\nSample of subwords:")
    print(subwords[:20])

    print("\nTop 20 words with highest weights:")
    top_weighted = sorted(weighted_words.items(), key=lambda x: x[1], reverse=True)[:10000]
    for word, weight in top_weighted:
        print(f"{word}: {weight}")

    print("\nBottom 20 words with lowest weights:")
    bottom_weighted = sorted(weighted_words.items(), key=lambda x: x[1])[:20]
    for word, weight in bottom_weighted:
        print(f"{word}: {weight}")

# 使用函數
file_path = 'C:\\Users\\User\\Desktop\\bpe_simple_vocab_16e6.txt'
analyze_vocab(file_path)