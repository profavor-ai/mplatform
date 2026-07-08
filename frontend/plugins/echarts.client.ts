import { defineNuxtPlugin } from '#app'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, GridComponent } from 'echarts/components'

export default defineNuxtPlugin((nuxtApp) => {
  use([CanvasRenderer, LineChart, TitleComponent, TooltipComponent, GridComponent])
  nuxtApp.vueApp.component('v-chart', VChart)
})
