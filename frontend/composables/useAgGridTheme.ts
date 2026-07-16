import { computed } from 'vue'
import 'ag-grid-enterprise'
import { themeQuartz, colorSchemeDark } from 'ag-grid-community'
import { useColors } from 'vuestic-ui'

/**
 * AG Grid 테마 및 공통 옵션을 중앙에서 관리하는 composable.
 * 
 * 테마나 공통 동작을 변경하고 싶으면 이 파일 하나만 수정하면 됩니다.
 * Vuestic UI의 다크모드 프리셋에 연동하여 자동으로 라이트/다크 테마를 전환합니다.
 *
 * 사용법:
 *   const { gridTheme, autoSizeStrategy } = useAgGridTheme()
 *   <ag-grid-vue :theme="gridTheme" :autoSizeStrategy="autoSizeStrategy" ... />
 */
export function useAgGridTheme() {
  const { currentPresetName } = useColors()

  const gridTheme = computed(() => {
    return currentPresetName.value === 'dark'
      ? themeQuartz.withPart(colorSchemeDark)
      : themeQuartz
  })

  /** 그리드 크기 변경 시 컬럼을 자동으로 그리드 너비에 맞춤 */
  const autoSizeStrategy = {
    type: 'fitGridWidth' as const,
    defaultMinWidth: 80,
  }

  return { gridTheme, autoSizeStrategy }
}
