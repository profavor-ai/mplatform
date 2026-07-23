import { useI18n } from 'vue-i18n'

export function getMultilingualText(val: any, locale?: string): string {
  if (!val) return ''

  let currentLocale = locale
  if (!currentLocale) {
    try {
      const { locale: i18nLocale } = useI18n()
      currentLocale = i18nLocale.value || 'ko'
    } catch {
      currentLocale = 'ko'
    }
  }

  if (typeof val === 'object' && val !== null) {
    return val[currentLocale] || val['ko'] || val['en'] || Object.values(val)[0] || ''
  }

  if (typeof val === 'string') {
    const trimmed = val.trim()
    if (trimmed.startsWith('{') && trimmed.endsWith('}')) {
      try {
        const parsed = JSON.parse(trimmed)
        if (typeof parsed === 'object' && parsed !== null) {
          return parsed[currentLocale] || parsed['ko'] || parsed['en'] || Object.values(parsed)[0] || val
        }
      } catch {
        return val
      }
    }
    return val
  }

  return String(val)
}
