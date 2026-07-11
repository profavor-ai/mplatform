export const useLoading = () => {
  const isLoading = useState('globalLoading', () => false)
  const loadingText = useState('globalLoadingText', () => '')

  const showLoading = (text = 'Loading...') => {
    loadingText.value = text
    isLoading.value = true
  }

  const hideLoading = () => {
    isLoading.value = false
  }

  return {
    isLoading,
    loadingText,
    showLoading,
    hideLoading
  }
}
