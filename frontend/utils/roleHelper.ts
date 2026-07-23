import { useRoles } from '~/composables/useRoles'

export function formatRoleWithLabel(code: string): string {
  const { formatRoleText } = useRoles()
  return formatRoleText(code)
}
