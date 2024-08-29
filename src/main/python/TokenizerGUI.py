import tkinter as tk
from tkinter import ttk

from novelai_api.tokenizers.simple_tokenizer import SimpleTokenizer


class TokenizerGUI:
    def __init__(self, master):
        self.master = master
        master.title("Tokenizer GUI")

        # 創建左側輸入框
        self.input_label = ttk.Label(master, text="Input Text:")
        self.input_label.grid(row=0, column=0, sticky="w", padx=5, pady=5)

        self.input_text = tk.Text(master, height=10, width=40)
        self.input_text.grid(row=1, column=0, padx=5, pady=5)

        # 創建右側向量顯示框
        self.vector_label = ttk.Label(master, text="Token Vector:")
        self.vector_label.grid(row=0, column=1, sticky="w", padx=5, pady=5)

        self.vector_text = tk.Text(master, height=10, width=40, state="disabled")
        self.vector_text.grid(row=1, column=1, padx=5, pady=5)

        # 綁定輸入框的按鍵事件
        self.input_text.bind("<KeyRelease>", self.update_vector)

        # 初始化tokenizer
        self.tokenizer = SimpleTokenizer()

    def update_vector(self, event):
        # 獲取輸入文本
        input_text = self.input_text.get("1.0", "end-1c")

        # 使用tokenizer進行編碼
        tokens = self.tokenizer.encode(input_text)

        # 更新向量顯示
        self.vector_text.config(state="normal")
        self.vector_text.delete("1.0", "end")
        self.vector_text.insert("1.0", str(tokens))
        self.vector_text.config(state="disabled")

root = tk.Tk()
gui = TokenizerGUI(root)
root.mainloop()