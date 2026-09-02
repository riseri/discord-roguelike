import type { ReactNode } from 'react'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Progress } from '@/components/ui/progress'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'

type WindowElement = 'div' | 'header' | 'section'

interface JrpgWindowProps {
  as?: WindowElement
  children: ReactNode
  className?: string
  'aria-label'?: string
  'aria-labelledby'?: string
}

export function JrpgWindow({
  as: Element = 'div',
  children,
  className = '',
  ...accessibleName
}: JrpgWindowProps) {
  return (
    <Element className={`jrpg-window ${className}`.trim()} {...accessibleName}>
      {children}
    </Element>
  )
}

interface HpBarProps {
  current: number
  maximum: number
  label: string
  tone?: 'player' | 'enemy'
}

export function HpBar({ current, maximum, label, tone = 'player' }: HpBarProps) {
  const percentage = maximum > 0 ? Math.max(0, Math.min(100, (current / maximum) * 100)) : 0

  return (
    <div className="vital-row">
      <div className="vital-row__label">
        <span>HP</span>
        <strong>{current} / {maximum}</strong>
      </div>
      <Progress
        className={`hp-bar hp-bar--${tone}`}
        value={percentage}
        aria-label={label}
      />
    </div>
  )
}

export function BlockMeter({ value }: { value: number }) {
  // The capped scale communicates relative Block at a glance without defining a gameplay maximum.
  const displayPercentage = Math.min(100, value * 5)

  return (
    <div className={`block-meter${value > 0 ? ' block-meter--active' : ''}`} aria-label={`${value} Block`}>
      <div className="block-meter__label">
        <span><span className="block-meter__icon" aria-hidden="true">◆</span> Block</span>
        <strong>{value}</strong>
      </div>
      <Progress className="block-bar" value={displayPercentage} aria-hidden="true" />
    </div>
  )
}

interface CombatantStatusProps {
  name: string
  label: string
  currentHp: number
  maxHp: number
  hpLabel: string
  tone: 'player' | 'enemy'
  block?: number
  marker?: ReactNode
  className?: string
}

export function CombatantStatus({
  name,
  label,
  currentHp,
  maxHp,
  hpLabel,
  tone,
  block,
  marker,
  className = '',
}: CombatantStatusProps) {
  return (
    <JrpgWindow
      className={`combatant-status combatant-status--${tone} ${className}`.trim()}
      aria-label={`${name} status`}
    >
      <div className="combatant-status__heading">
        <div>
          <p className="ui-label">{label}</p>
          <h2>{name}</h2>
        </div>
        {marker}
      </div>
      <HpBar current={currentHp} maximum={maxHp} label={hpLabel} tone={tone} />
      {block !== undefined && <BlockMeter value={block} />}
    </JrpgWindow>
  )
}

interface EnemyIntentProps {
  name: string
  damage?: number
}

export function EnemyIntent({ name, damage }: EnemyIntentProps) {
  return (
    <div className={`enemy-intent${damage !== undefined ? ' enemy-intent--danger' : ''}`}>
      <span className="enemy-intent__icon" aria-hidden="true">!</span>
      <span className="enemy-intent__copy">
        <span className="ui-label">Next action</span>
        <strong>{name}</strong>
      </span>
      <span className="enemy-intent__value">{damage !== undefined ? `${damage} DMG` : '—'}</span>
    </div>
  )
}

export function TargetCursor({ selected }: { selected: boolean }) {
  return (
    <span className={`target-cursor${selected ? ' target-cursor--visible' : ''}`} aria-hidden="true">
      <span>▼</span> Target
    </span>
  )
}

interface CommandMenuProps {
  value: string
  onValueChange: (value: string) => void
  children: ReactNode
  disabled?: boolean
}

export function CommandMenu({ value, onValueChange, children, disabled }: CommandMenuProps) {
  return (
    <ToggleGroup
      className="ability-list"
      type="single"
      orientation="vertical"
      value={value}
      onValueChange={(nextValue) => nextValue && onValueChange(nextValue)}
      disabled={disabled}
      aria-label="Knight commands"
    >
      {children}
    </ToggleGroup>
  )
}

interface CommandButtonProps {
  value: string
  children: ReactNode
  icon?: ReactNode
}

export function CommandButton({ value, children, icon }: CommandButtonProps) {
  return (
    <ToggleGroupItem className="command-button" value={value}>
      <span className="command-button__cursor" aria-hidden="true">▸</span>
      {icon && <span className="command-button__icon" aria-hidden="true">{icon}</span>}
      <strong>{children}</strong>
    </ToggleGroupItem>
  )
}

interface TargetButtonProps {
  name: string
  selected: boolean
  disabled: boolean
  onSelect: () => void
}

export function TargetButton({ name, selected, disabled, onSelect }: TargetButtonProps) {
  return (
    <Button
      className="target-button"
      variant="ghost"
      type="button"
      aria-pressed={selected}
      disabled={disabled}
      onClick={onSelect}
    >
      <span className="target-button__cursor" aria-hidden="true">▸</span>
      <span className="target-button__portrait" aria-hidden="true">{name.charAt(0)}</span>
      <strong>{name}</strong>
      <span>{selected ? 'Selected' : 'Choose'}</span>
    </Button>
  )
}

interface ResultWindowProps {
  status: 'WON' | 'LOST'
  pending: boolean
  onRestart: () => void
}

export function ResultWindow({ status, pending, onRestart }: ResultWindowProps) {
  const won = status === 'WON'

  return (
    <Dialog open>
      <DialogContent className="jrpg-window result-window" showCloseButton={false}>
        <DialogHeader>
          <p className="ui-label">Encounter complete</p>
          <DialogTitle>{won ? 'Victory!' : 'Defeated'}</DialogTitle>
          <DialogDescription>
            {won ? 'The road ahead is clear.' : 'The Knight has fallen.'}
          </DialogDescription>
        </DialogHeader>
        <Button className="secondary-button" type="button" onClick={onRestart} disabled={pending}>
          {pending ? 'Preparing…' : 'Start another encounter'}
        </Button>
      </DialogContent>
    </Dialog>
  )
}
