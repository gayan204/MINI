package com.example.mini.editor

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

class UndoRedoManager(private val editText: EditText) : TextWatcher {
    private val undoStack = mutableListOf<String>()
    private val redoStack = mutableListOf<String>()
    private var isUndoRedoAction = false
    private var lastPushTime = 0L
    private val DEBOUNCE_MS = 1000L

    init {
        // Initial state
        undoStack.add("")
    }

    fun undo() {
        if (undoStack.size > 1) {
            val currentState = undoStack.removeAt(undoStack.size - 1)
            redoStack.add(currentState)
            
            val previousState = undoStack.last()
            applyState(previousState)
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val nextState = redoStack.removeAt(redoStack.size - 1)
            undoStack.add(nextState)
            
            applyState(nextState)
        }
    }

    private fun applyState(state: String) {
        isUndoRedoAction = true
        editText.setText(state)
        editText.setSelection(state.length)
        isUndoRedoAction = false
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

    override fun afterTextChanged(s: Editable?) {
        if (!isUndoRedoAction && s != null) {
            val currentText = s.toString()
            if (undoStack.isEmpty() || undoStack.last() != currentText) {
                val now = System.currentTimeMillis()
                val isWordBoundary = currentText.endsWith(" ") || currentText.endsWith("\n")
                if (now - lastPushTime > DEBOUNCE_MS || isWordBoundary) {
                    undoStack.add(currentText)
                    redoStack.clear()
                    lastPushTime = now
                    
                    // Prevent infinite growth
                    if (undoStack.size > 50) {
                        undoStack.removeAt(0)
                    }
                } else if (undoStack.isNotEmpty()) {
                    // Update the top of the stack instead of pushing a new item
                    undoStack[undoStack.size - 1] = currentText
                }
            }
        }
    }
}
