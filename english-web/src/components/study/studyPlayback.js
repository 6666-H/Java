function canUseBrowserAudio() {
  return typeof window !== 'undefined'
}

function speakWord(wordText) {
  if (!canUseBrowserAudio() || !wordText || !window.speechSynthesis) {
    return Promise.resolve(false)
  }

  return new Promise((resolve) => {
    const utterance = new SpeechSynthesisUtterance(wordText)
    utterance.lang = 'en-US'
    utterance.onend = () => resolve(true)
    utterance.onerror = () => resolve(false)
    window.speechSynthesis.cancel()
    window.speechSynthesis.speak(utterance)
  })
}

export function playWordAudio(word, options = {}) {
  const { enabled = true, force = false } = options
  if (!canUseBrowserAudio() || (!enabled && !force)) {
    return Promise.resolve(false)
  }

  if (word?.audioUrl) {
    try {
      const audio = new Audio(word.audioUrl)
      return audio.play().then(() => true).catch(() => speakWord(word?.word))
    } catch (_) {
      return speakWord(word?.word)
    }
  }

  return speakWord(word?.word)
}

export function playSuccessTone(enabled = true) {
  if (!enabled || !canUseBrowserAudio()) return

  try {
    const context = new (window.AudioContext || window.webkitAudioContext)()
    const oscillator = context.createOscillator()
    const gainNode = context.createGain()

    oscillator.connect(gainNode)
    gainNode.connect(context.destination)

    oscillator.type = 'sine'
    oscillator.frequency.setValueAtTime(720, context.currentTime)
    oscillator.frequency.linearRampToValueAtTime(900, context.currentTime + 0.16)

    gainNode.gain.setValueAtTime(0.0001, context.currentTime)
    gainNode.gain.linearRampToValueAtTime(0.16, context.currentTime + 0.03)
    gainNode.gain.exponentialRampToValueAtTime(0.001, context.currentTime + 0.28)

    oscillator.start(context.currentTime)
    oscillator.stop(context.currentTime + 0.28)
  } catch (_) {}
}

export function vibrateLight(duration = 120) {
  if (!canUseBrowserAudio() || !navigator?.vibrate) return
  navigator.vibrate(duration)
}
