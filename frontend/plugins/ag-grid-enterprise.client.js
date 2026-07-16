import 'ag-grid-enterprise'
import { LicenseManager, AllEnterpriseModule, ModuleRegistry } from 'ag-grid-enterprise'

export default defineNuxtPlugin(() => {
  const config = useRuntimeConfig()
  LicenseManager.setLicenseKey(config.public.agGridLicense)
  ModuleRegistry.registerModules([AllEnterpriseModule]);
})
