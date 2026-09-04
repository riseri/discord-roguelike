import { useEffect, useRef, useState } from 'react'
import type { CSSProperties } from 'react'
import {
  CheckIcon,
  CrownIcon,
  DoorOpenIcon,
  FootprintsIcon,
  GemIcon,
  ScrollTextIcon,
  ShieldIcon,
  SparklesIcon,
  SwordIcon,
} from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Separator } from '@/components/ui/separator'
import './App.css'
import './components/game-ui/GameUi.css'
import {
  CombatantStatus,
  CommandButton,
  CommandMenu,
  JrpgWindow,
  ResultWindow,
} from './components/game-ui/GameUi'

type AbilityId = 'SLASH' | 'GUARD' | 'SHIELD_BASH'
type CombatStatus = 'ACTIVE' | 'WON' | 'LOST'
type RoomType = 'COMBAT' | 'EVENT' | 'TREASURE' | 'BOSS'
type RunScreen = 'LOADING' | 'NO_RUN' | 'CURRENT_ROOM' | 'COMBAT' | 'ROOM_COMPLETE' | 'RUN_COMPLETE'

interface RoomSummary {
  id: string
  type: RoomType
}

interface RunState {
  seed: number
  status: 'ACTIVE' | 'WON' | 'LOST'
  playerHp: number
  playerMaxHp: number
  currentRoomId: string
  currentRoom: RoomSummary
  availableNextRooms: RoomSummary[]
  completedRoomIds: string[]
  ownedRelicIds: string[]
  rngState: number
}

interface PlayerState {
  entityId: string
  currentHp: number
  maxHp: number
  block: number
  position: Position
}

interface Position { x: number; y: number }

interface EnemyIntention {
  id: string
  damage: number
}

interface EnemyState {
  entityId: string
  contentId: string
  currentHp: number
  maxHp: number
  intention: EnemyIntention | null
  position: Position
  stunnedTurns: number
}

interface CombatState {
  player: PlayerState
  enemies: EnemyState[]
  abilities: Ability[]
  phase: 'PLAYER' | 'ENEMY'
  status: CombatStatus
  grid: { width: number; height: number }
  reachablePositions: Position[]
}

interface Ability {
  id: AbilityId
  name: string
  description: string
  target: 'ENEMY' | 'SELF'
}

interface ApiError {
  code?: string
  message?: string
}

type CombatEvent = {
  type: 'ENTITY_MOVED' | 'ENTITY_STUNNED' | 'ABILITY_USED' | 'DAMAGE_DEALT' | 'BLOCK_GAINED' | 'ENEMY_INTENTION_GENERATED' |
    'ENEMY_ACTION_USED' | 'BLOCK_ABSORBED' | 'ENTITY_DEFEATED' | 'COMBAT_WON' | 'COMBAT_LOST'
  actorId?: string
  abilityId?: AbilityId
  sourceId?: string
  targetId?: string
  entityId?: string
  enemyId?: string
  intention?: EnemyIntention
  intentionId?: string
  amount?: number
  from?: Position
  to?: Position
  turns?: number
}

interface CombatActionResponse {
  state: CombatState
  events: CombatEvent[]
}

function displayName(identifier: string) {
  return identifier
    .replaceAll('-', ' ')
    .replaceAll('_', ' ')
    .replace(/\b\w/g, (letter) => letter.toUpperCase())
}

const roomTypeDetails: Record<RoomType, { description: string; arrival: string; title: string; icon: typeof SwordIcon }> = {
  COMBAT: {
    description: 'Face an enemy encounter',
    arrival: 'Enemies block the path ahead. Steel your resolve before stepping into the encounter.',
    title: 'Goblin Ambush',
    icon: SwordIcon,
  },
  EVENT: {
    description: 'Meet an uncertain fate',
    arrival: 'A strange encounter waits beyond the bend. Choose your next step carefully.',
    title: 'An Uncertain Meeting',
    icon: ScrollTextIcon,
  },
  TREASURE: {
    description: 'Claim a hidden reward',
    arrival: 'A forgotten cache lies ahead, untouched beneath the old stones.',
    title: 'A Hidden Cache',
    icon: GemIcon,
  },
  BOSS: {
    description: 'Challenge the dungeon boss',
    arrival: 'The dungeon lord waits beyond the final gate. There will be no turning back.',
    title: 'The Final Gate',
    icon: CrownIcon,
  },
}

class ApiRequestError extends Error {
  readonly code?: string

  constructor(
    message: string,
    code?: string,
  ) {
    super(message)
    this.code = code
  }
}

async function requestApi<T>(
  path: string,
  method: 'GET' | 'POST',
  body?: unknown,
): Promise<T> {
  const response = await fetch(path, {
    method,
    ...(body !== undefined ? {
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    } : {}),
  })

  if (!response.ok) {
    const error = (await response.json().catch(() => ({}))) as ApiError
    throw new ApiRequestError(
      error.message || 'The server could not complete that action.',
      error.code,
    )
  }

  return (await response.json()) as T
}

const messageDelay = 360
const stateChangeDelay = 220

function waitForPresentation(delay: number) {
  return new Promise((resolve) => window.setTimeout(resolve, delay))
}

// Event amounts are authoritative deltas, allowing the UI to reveal state without replaying rules.
function applyEvent(state: CombatState, event: CombatEvent): CombatState {
  const amount = event.amount ?? 0
  if (event.type === 'ENTITY_MOVED' && event.entityId && event.to) {
    if (event.entityId === state.player.entityId) return { ...state, player: { ...state.player, position: event.to } }
    return { ...state, enemies: state.enemies.map((enemy) => enemy.entityId === event.entityId ? { ...enemy, position: event.to! } : enemy) }
  }
  if (event.type === 'DAMAGE_DEALT' && event.targetId) {
    if (event.targetId === state.player.entityId) {
      return { ...state, player: { ...state.player, currentHp: Math.max(0, state.player.currentHp - amount) } }
    }
    return {
      ...state,
      enemies: state.enemies.map((enemy) => enemy.entityId === event.targetId
        ? { ...enemy, currentHp: Math.max(0, enemy.currentHp - amount) }
        : enemy),
    }
  }
  if (event.type === 'BLOCK_GAINED' && event.entityId === state.player.entityId) {
    return { ...state, player: { ...state.player, block: state.player.block + amount } }
  }
  if (event.type === 'BLOCK_ABSORBED' && event.entityId === state.player.entityId) {
    return { ...state, player: { ...state.player, block: Math.max(0, state.player.block - amount) } }
  }
  if (event.type === 'ENEMY_INTENTION_GENERATED' && event.enemyId && event.intention) {
    return {
      ...state,
      enemies: state.enemies.map((enemy) => enemy.entityId === event.enemyId
        ? { ...enemy, intention: event.intention ?? null }
        : enemy),
    }
  }
  if (event.type === 'ENEMY_ACTION_USED' && event.enemyId) {
    return {
      ...state,
      phase: 'ENEMY',
      enemies: state.enemies.map((enemy) => enemy.entityId === event.enemyId
        ? { ...enemy, intention: null }
        : enemy),
    }
  }
  if (event.type === 'COMBAT_WON') return { ...state, status: 'WON' }
  if (event.type === 'COMBAT_LOST') return { ...state, status: 'LOST' }
  return state
}

function eventMessage(event: CombatEvent, state: CombatState) {
  if (event.type === 'ENTITY_MOVED') return event.entityId === state.player.entityId ? 'Knight advances' : 'Enemy advances'
  if (event.type === 'ENTITY_STUNNED') return 'Enemy stunned'
  if (event.type === 'ABILITY_USED') return `Knight uses ${displayName(event.abilityId ?? 'ability')}`
  if (event.type === 'DAMAGE_DEALT') return `${event.amount ?? 0} damage`
  if (event.type === 'BLOCK_GAINED') return `Guard raised · +${event.amount ?? 0} Block`
  if (event.type === 'BLOCK_ABSORBED') return `Block absorbs ${event.amount ?? 0}`
  if (event.type === 'ENEMY_ACTION_USED') {
    const enemy = state.enemies.find(({ entityId }) => entityId === event.enemyId)
    return `${displayName(enemy?.contentId ?? 'Enemy')} uses ${displayName(event.intentionId ?? 'Attack')}`
  }
  if (event.type === 'ENTITY_DEFEATED') return event.entityId === state.player.entityId ? 'Knight falls' : 'Enemy defeated'
  if (event.type === 'COMBAT_WON') return 'Victory!'
  if (event.type === 'COMBAT_LOST') return 'Defeated'
  return null
}

function App() {
  const [screen, setScreen] = useState<RunScreen>('LOADING')
  const [run, setRun] = useState<RunState | null>(null)
  const [combat, setCombat] = useState<CombatState | null>(null)
  const [selectedAbility, setSelectedAbility] = useState<AbilityId>('SLASH')
  const [selectedTarget, setSelectedTarget] = useState<string | null>(null)
  const [pending, setPending] = useState(false)
  const [presentation, setPresentation] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const actionInFlight = useRef(false)

  const selectInitialTarget = (nextCombat: CombatState) => {
    setSelectedTarget(nextCombat.enemies.find((enemy) => enemy.currentHp > 0)?.entityId ?? null)
  }

  const showAuthoritativeRun = (nextRun: RunState) => {
    setRun(nextRun)
    setCombat(null)
    setScreen(
      nextRun.status !== 'ACTIVE'
        ? 'RUN_COMPLETE'
        : nextRun.completedRoomIds.includes(nextRun.currentRoomId) ? 'ROOM_COMPLETE' : 'CURRENT_ROOM',
    )
  }

  useEffect(() => {
    let cancelled = false

    const loadCurrentState = async () => {
      try {
        const currentRun = await requestApi<RunState>('/api/runs/current', 'GET')
        if (cancelled) return
        setRun(currentRun)

        if (currentRun.completedRoomIds.includes(currentRun.currentRoomId)) {
          setScreen('ROOM_COMPLETE')
          return
        }

        try {
          const currentCombat = await requestApi<CombatState>('/api/combat', 'GET')
          if (cancelled) return
          setCombat(currentCombat)
          selectInitialTarget(currentCombat)
          setScreen('COMBAT')
        } catch (requestError) {
          if (cancelled) return
          if (requestError instanceof ApiRequestError && requestError.code === 'NO_ACTIVE_COMBAT') {
            setScreen('CURRENT_ROOM')
          } else {
            throw requestError
          }
        }
      } catch (requestError) {
        if (cancelled) return
        if (requestError instanceof ApiRequestError && requestError.code === 'NO_ACTIVE_RUN') {
          setScreen('NO_RUN')
        } else {
          setError(requestError instanceof Error ? requestError.message : 'Unable to reach the game server.')
          setScreen('NO_RUN')
        }
      }
    }

    void loadCurrentState()
    return () => { cancelled = true }
  }, [])

  const startRun = async () => {
    setPending(true)
    setError(null)
    try {
      const nextRun = await requestApi<RunState>('/api/runs', 'POST')
      showAuthoritativeRun(nextRun)
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Unable to reach the game server.')
    } finally {
      setPending(false)
    }
  }

  const enterCombat = async () => {
    setPending(true)
    setError(null)
    try {
      const nextCombat = await requestApi<CombatState>('/api/combat', 'POST')
      setCombat(nextCombat)
      selectInitialTarget(nextCombat)
      setScreen('COMBAT')
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Unable to reach the game server.')
    } finally {
      setPending(false)
    }
  }

  const chooseRoom = async (roomId: string) => {
    setPending(true)
    setError(null)
    try {
      const nextRun = await requestApi<RunState>('/api/runs/current/rooms', 'POST', { roomId })
      showAuthoritativeRun(nextRun)
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Unable to choose that path.')
    } finally {
      setPending(false)
    }
  }

  const useAbility = async () => {
    if (!combat || actionInFlight.current) return

    const ability = combat.abilities.find(({ id }) => id === selectedAbility)
    const targetId = ability?.target === 'SELF' ? combat.player.entityId : selectedTarget
    if (!targetId) {
      setError('Select an enemy target first.')
      return
    }

    actionInFlight.current = true
    setPending(true)
    setError(null)
    try {
      const result = await requestApi<CombatActionResponse>('/api/combat/actions', 'POST', {
        abilityId: selectedAbility,
        targetId,
      })
      let displayedCombat = combat
      for (const event of result.events) {
        const message = eventMessage(event, displayedCombat)
        if (message) {
          setPresentation(message)
          await waitForPresentation(messageDelay)
        }
        displayedCombat = applyEvent(displayedCombat, event)
        setCombat(displayedCombat)
        if (message) await waitForPresentation(stateChangeDelay)
      }
      setCombat(result.state)
      setPresentation(null)
      const currentTarget = result.state.enemies.find(
        (enemy) => enemy.entityId === selectedTarget && enemy.currentHp > 0,
      )
      setSelectedTarget(
        currentTarget?.entityId ??
          result.state.enemies.find((enemy) => enemy.currentHp > 0)?.entityId ??
          null,
      )
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Unable to reach the game server.')
    } finally {
      actionInFlight.current = false
      setPresentation(null)
      setPending(false)
    }
  }

  const moveKnight = async (destination: Position) => {
    if (!combat || actionInFlight.current) return
    actionInFlight.current = true
    setPending(true)
    setError(null)
    try {
      const result = await requestApi<CombatActionResponse>('/api/combat/actions', 'POST', { destination })
      setCombat(result.state)
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Unable to move there.')
    } finally {
      actionInFlight.current = false
      setPending(false)
    }
  }

  const leaveCombat = async () => {
    setPending(true)
    setError(null)
    try {
      const currentRun = await requestApi<RunState>('/api/runs/current', 'GET')
      showAuthoritativeRun(currentRun)
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Unable to reach the game server.')
    } finally {
      setPending(false)
    }
  }

  if (screen === 'LOADING') {
    return (
      <main className="run-shell run-shell--centered" aria-busy="true">
        <JrpgWindow as="section" className="run-window run-window--loading">
          <div className="crest crest--loading" aria-hidden="true">✦</div>
          <p className="eyebrow">Discord Roguelike</p>
          <h1>Loading expedition…</h1>
        </JrpgWindow>
      </main>
    )
  }

  if (screen === 'NO_RUN') {
    return (
      <main className="run-shell run-shell--centered">
        <JrpgWindow as="section" className="run-window run-window--welcome">
          <div className="crest" aria-hidden="true">✦</div>
          <p className="eyebrow">Discord Roguelike</p>
          <h1>A Road Into Ruin</h1>
          <p className="intro">Take up the Knight&apos;s shield and begin a short expedition into the old dungeon road.</p>
          <Button className="primary-button" type="button" onClick={startRun} disabled={pending}>
            {pending ? 'Starting expedition…' : 'Start run'}
          </Button>
          {error && <p className="error-message" role="alert">{error}</p>}
        </JrpgWindow>
      </main>
    )
  }

  if (run && (screen === 'CURRENT_ROOM' || screen === 'ROOM_COMPLETE' || screen === 'RUN_COMPLETE')) {
    const roomComplete = screen === 'ROOM_COMPLETE'
    const runComplete = screen === 'RUN_COMPLETE'
    const currentRoomDetails = roomTypeDetails[run.currentRoom.type]
    const CurrentRoomIcon = currentRoomDetails.icon
    return (
      <main className="run-shell">
        <JrpgWindow as="header" className="run-header">
          <div>
            <p className="eyebrow">Knight expedition</p>
            <strong>Run {String(run.seed).slice(-6)}</strong>
          </div>
          <div className="run-header__vital" aria-label={`Knight health ${run.playerHp} of ${run.playerMaxHp}`}>
            <ShieldIcon size={17} aria-hidden="true" />
            <span>HP</span>
            <strong>{run.playerHp} / {run.playerMaxHp}</strong>
          </div>
        </JrpgWindow>

        <section className="room-stage" aria-labelledby="room-title">
          <div className="room-stage__sun" aria-hidden="true" />
          <JrpgWindow as="section" className={`room-window${roomComplete ? ' room-window--complete' : ''}${runComplete ? ' room-window--failed' : ''}`}>
            <div className="room-icon" aria-hidden="true">
              {roomComplete ? <SparklesIcon size={28} /> : runComplete ? <ShieldIcon size={28} /> : <CurrentRoomIcon size={28} />}
            </div>
            <p className="eyebrow">{roomComplete ? 'Room complete' : runComplete ? 'Run ended' : 'Current room'}</p>
            <h1 id="room-title">{roomComplete ? 'The Road Is Clear' : runComplete ? 'The Knight Has Fallen' : currentRoomDetails.title}</h1>
            <p className="room-location">The Old Dungeon Road</p>
            <Separator className="room-separator" />
            <p className={`intro${roomComplete ? ' route-intro' : ''}`}>
              {roomComplete
                ? 'The encounter is over. Choose where the Knight ventures next.'
                : runComplete
                  ? 'The old road has claimed this expedition. Your progress ends here.'
                  : currentRoomDetails.arrival}
            </p>
            {roomComplete && run.availableNextRooms.length > 0 && (
              <div className="route-selection" aria-labelledby="route-selection-title">
                <div className="route-current" aria-label={`Completed ${displayName(run.currentRoom.type)} room`}>
                  <span className={`route-room__icon route-room__icon--${run.currentRoom.type.toLowerCase()}`} aria-hidden="true">
                    <CurrentRoomIcon size={20} />
                  </span>
                  <span>
                    <small>Current · Complete</small>
                    <strong>{displayName(run.currentRoom.type)}</strong>
                  </span>
                  <CheckIcon className="route-current__check" size={18} aria-hidden="true" />
                </div>
                <span className="route-connector" aria-hidden="true" />
                <h2 id="route-selection-title">Choose your path</h2>
                <div className="route-choices">
                  {run.availableNextRooms.map((room) => {
                    const details = roomTypeDetails[room.type]
                    const RoomIcon = details.icon
                    return (
                      <Button
                        className={`route-room route-room--${room.type.toLowerCase()}`}
                        variant="ghost"
                        type="button"
                        key={room.id}
                        onClick={() => void chooseRoom(room.id)}
                        disabled={pending}
                      >
                        <span className={`route-room__icon route-room__icon--${room.type.toLowerCase()}`} aria-hidden="true">
                          <RoomIcon size={24} />
                        </span>
                        <span className="route-room__copy">
                          <small>Available route</small>
                          <strong>{displayName(room.type)}</strong>
                          <span>{details.description}</span>
                        </span>
                        <span className="route-room__action">{pending ? 'Choosing…' : 'Choose'} <span aria-hidden="true">›</span></span>
                      </Button>
                    )
                  })}
                </div>
              </div>
            )}
            {!roomComplete && !runComplete && (
              <Button className="primary-button room-action" type="button" onClick={enterCombat} disabled={pending}>
                <DoorOpenIcon size={16} aria-hidden="true" />
                {pending ? 'Entering combat…' : 'Enter combat'}
              </Button>
            )}
            {error && <p className="error-message" role="alert">{error}</p>}
          </JrpgWindow>
        </section>

        <footer className="run-footer">
          <span>Room · {displayName(run.currentRoomId)}</span>
          <span>{roomComplete ? 'Encounter cleared' : runComplete ? 'Expedition ended' : `${displayName(run.currentRoom.type)} ahead`}</span>
        </footer>
      </main>
    )
  }

  if (!combat) return null

  const selectedAbilityDetails = combat.abilities.find(({ id }) => id === selectedAbility)
  const canAct = combat.status === 'ACTIVE' && combat.phase === 'PLAYER' && !pending
  const selectedEnemy = combat.enemies.find(({ entityId }) => entityId === selectedTarget)
  const targetInRange = selectedEnemy ? Math.abs(selectedEnemy.position.x - combat.player.position.x) + Math.abs(selectedEnemy.position.y - combat.player.position.y) === 1 : false
  const hasTarget = selectedAbilityDetails?.target === 'SELF' || (selectedTarget !== null && targetInRange)
  const reachable = new Set(combat.reachablePositions.map(({ x, y }) => `${x},${y}`))
  const enemiesByTile = new Map(combat.enemies.filter(({ currentHp }) => currentHp > 0).map((enemy) => [`${enemy.position.x},${enemy.position.y}`, enemy]))

  return (
    <main className="combat-shell">
      <JrpgWindow as="header" className="combat-header">
        <div className="combat-header__title">
          <h1>Goblin Ambush</h1>
          <p>The Old Dungeon Road</p>
        </div>
        <div className={`phase-badge phase-badge--${combat.status.toLowerCase()}`} role="status">
          {combat.status === 'ACTIVE' && (presentation ?? (pending ? 'Resolving…' : 'Your turn'))}
          {combat.status === 'WON' && 'Victory'}
          {combat.status === 'LOST' && 'Defeated'}
        </div>
        <span className="encounter-kind">Solo encounter</span>
      </JrpgWindow>

      {combat.status !== 'ACTIVE' && (
        <ResultWindow
          status={combat.status}
          pending={pending}
          actionLabel={combat.status === 'WON' ? 'Review cleared room' : 'View run result'}
          onContinue={leaveCombat}
        />
      )}

      <section className="tactical-layout" aria-label="Tactical battlefield">
        <JrpgWindow as="aside" className="tactical-roster">
          <CombatantStatus name="Knight" label="You" currentHp={combat.player.currentHp} maxHp={combat.player.maxHp} hpLabel="Knight health" tone="player" block={combat.player.block} />
          {combat.enemies.map((enemy) => <CombatantStatus key={enemy.entityId} className={selectedTarget === enemy.entityId ? 'enemy-status enemy-status--selected' : 'enemy-status'} name={displayName(enemy.entityId)} label={enemy.stunnedTurns ? 'Stunned' : `${displayName(enemy.intention?.id ?? 'waiting')} · ${enemy.intention?.damage ?? 0} dmg`} currentHp={enemy.currentHp} maxHp={enemy.maxHp} hpLabel={`${displayName(enemy.entityId)} health`} tone="enemy" />)}
        </JrpgWindow>
        <div className="tactical-board" style={{ '--grid-columns': combat.grid.width } as CSSProperties}>
          {Array.from({ length: combat.grid.width * combat.grid.height }, (_, index) => {
            const position = { x: index % combat.grid.width, y: Math.floor(index / combat.grid.width) }
            const key = `${position.x},${position.y}`
            const enemy = enemiesByTile.get(key)
            const isKnight = combat.player.position.x === position.x && combat.player.position.y === position.y
            const canMove = canAct && reachable.has(key) && !enemy
            return <button key={key} type="button" className={`tactical-tile${canMove ? ' tactical-tile--reachable' : ''}${enemy && selectedTarget === enemy.entityId ? ' tactical-tile--selected' : ''}`} onClick={() => enemy ? setSelectedTarget(enemy.entityId) : canMove ? void moveKnight(position) : undefined} disabled={pending} aria-label={enemy ? `${displayName(enemy.entityId)}, ${enemy.currentHp} health` : isKnight ? 'Knight' : canMove ? `Move to column ${position.x + 1}, row ${position.y + 1}` : `Column ${position.x + 1}, row ${position.y + 1}`}>
              {isKnight && <span className="tactical-unit tactical-unit--knight"><ShieldIcon size={22} /><b>Knight</b></span>}
              {enemy && <span className="tactical-unit tactical-unit--enemy"><span className="intent-pip">{enemy.stunnedTurns ? '✦' : enemy.intention?.damage}</span><b>{displayName(enemy.contentId)}</b><small>{enemy.currentHp}/{enemy.maxHp}</small></span>}
            </button>
          })}
        </div>
        <div className="tactical-legend"><span><i className="legend-swatch legend-swatch--move" /> Move range</span><span><i className="legend-swatch legend-swatch--target" /> Selected target</span></div>
      </section>

      {combat.status === 'ACTIVE' && (
        <JrpgWindow as="section" className="action-panel tactical-actions" aria-labelledby="ability-heading">
          <div className="command-menu">
            <p className="panel-prompt" id="ability-heading">Choose an action</p>
            <div className="move-hint"><FootprintsIcon size={16} /> Select a blue tile to move</div>
            <CommandMenu
              value={selectedAbility}
              onValueChange={(value) => setSelectedAbility(value as AbilityId)}
              disabled={!canAct}
            >
              {combat.abilities.map((ability) => (
                <CommandButton
                  key={ability.id}
                  value={ability.id}
                  icon={ability.id === 'GUARD' ? <ShieldIcon size={17} /> : <SwordIcon size={17} />}
                >
                  {ability.name}
                </CommandButton>
              ))}
            </CommandMenu>
          </div>
          <div className="command-detail" aria-live="polite">
            <div className="command-detail__title">
              {selectedAbility === 'GUARD' ? <ShieldIcon size={24} /> : <SwordIcon size={24} />}
              <h2>{selectedAbilityDetails?.name}</h2>
            </div>
            <p>{selectedAbilityDetails?.description}</p>
            <Separator className="command-separator" />
            <dl>
              <div><dt>Target</dt><dd>{selectedAbilityDetails?.target === 'SELF' ? 'Self' : 'One enemy'}</dd></div>
              <div><dt>Type</dt><dd>{selectedAbility === 'GUARD' ? 'Defense' : 'Attack'}</dd></div>
            </dl>
          </div>
          <div className="target-menu tactical-confirm">
            <p className="panel-prompt">{selectedAbilityDetails?.target === 'SELF' ? 'Ready to defend' : selectedEnemy ? `${displayName(selectedEnemy.entityId)} · ${targetInRange ? 'In range' : 'Out of range'}` : 'Select an adjacent enemy'}</p>
            <div className="target-menu__footer">
              <Button className="primary-button" type="button" onClick={useAbility} disabled={!canAct || !hasTarget}>
                {pending ? 'Resolving…' : 'Confirm action'}
              </Button>
            </div>
          </div>
          {error && (
            <p className="error-message" role="alert">
              {error}
            </p>
          )}
        </JrpgWindow>
      )}
    </main>
  )
}

export default App
