import re
from collections import Counter
import matplotlib.pyplot as plt
from typing import List, Tuple
import unicodedata

def load_bpe_vocab(file_path: str) -> List[str]:
    with open(file_path, 'r', encoding='utf-8') as file:
        next(file)  # 跳過版本行
        return [line.strip() for line in file]

def analyze_bpe_vocab(vocab: List[str]):
    print(f"Vocabulary size: {len(vocab)}")

    # 分類token
    full_words = [token for token in vocab if token.endswith('</w>')]
    subwords = [token for token in vocab if not token.endswith('</w>')]
    print(f"Full words: {len(full_words)}")
    print(f"Subwords: {len(subwords)}")

    # Token長度分析
    token_lengths = [len(token) for token in vocab]
    avg_length = sum(token_lengths) / len(vocab)
    print(f"Average token length: {avg_length:.2f}")
    plot_distribution(token_lengths, "Token Length Distribution", "Length", "Frequency")

    # 最常見的token
    print("Most common tokens:")
    print(Counter(vocab).most_common(20))

    # 分析子詞結構
    analyze_subword_structure(subwords)

    # 特殊字符分析
    analyze_special_characters(vocab)

    # 大小寫分析
    analyze_case(vocab)

    # 語言分析
    analyze_languages(vocab)

    # 數字分析
    analyze_numbers(vocab)

def analyze_subword_structure(subwords: List[str]):
    prefixes = Counter(token[:2] for token in subwords)
    suffixes = Counter(token[-2:] for token in subwords)
    print("Most common prefixes:")
    print(prefixes.most_common(10))
    print("Most common suffixes:")
    print(suffixes.most_common(10))

def analyze_special_characters(vocab: List[str]):
    special_chars = re.findall(r'[^a-zA-Z0-9\s]', ' '.join(vocab))
    char_counts = Counter(special_chars)
    print("Special characters:")
    print(char_counts.most_common(20))
    plot_distribution(char_counts.values(), "Special Character Distribution", "Character", "Frequency")

def analyze_case(vocab: List[str]):
    lowercase = sum(1 for token in vocab if token.islower())
    uppercase = sum(1 for token in vocab if token.isupper())
    mixed_case = len(vocab) - lowercase - uppercase
    print(f"Lowercase tokens: {lowercase}")
    print(f"Uppercase tokens: {uppercase}")
    print(f"Mixed case tokens: {mixed_case}")

def analyze_languages(vocab: List[str]):
    language_counts = Counter()
    for token in vocab:
        if re.match(r'^[a-zA-Z]+$', token):
            language_counts['Latin'] += 1
        elif re.match(r'^[\u0400-\u04FF]+$', token):
            language_counts['Cyrillic'] += 1
        elif re.match(r'^[\u4E00-\u9FFF]+$', token):
            language_counts['Chinese'] += 1
        elif re.match(r'^[\u3040-\u309F\u30A0-\u30FF]+$', token):
            language_counts['Japanese'] += 1
        elif re.match(r'^[\uAC00-\uD7A3]+$', token):
            language_counts['Korean'] += 1
        else:
            language_counts['Other'] += 1
    print("Language distribution:")
    print(language_counts)

def analyze_numbers(vocab: List[str]):
    number_tokens = [token for token in vocab if re.match(r'\d', token)]
    print(f"Number of tokens containing digits: {len(number_tokens)}")
    print("Sample number tokens:")
    print(number_tokens[:20])

def plot_distribution(data: List[int], title: str, xlabel: str, ylabel: str):
    plt.figure(figsize=(10, 6))
    plt.hist(data, bins=50)
    plt.title(title)
    plt.xlabel(xlabel)
    plt.ylabel(ylabel)
    plt.show()

# 使用函數
vocab = load_bpe_vocab('C:\\Users\\User\\Desktop\\bpe_simple_vocab_16e6.txt')
analyze_bpe_vocab(vocab)