# Sony MDR protocol notes

What LibreCans speaks, collected from Gadgetbridge, SonyHeadphonesClient (Plutoberth and mos9527) and a
real WF-1000XM5 capture (firmware 6.1.0). Byte values are hex.

## Transport

RFCOMM to service record `956C7B26-D49A-4BA8-B03F-B17D393CB6E2` (v2) or
`96CC203E-5068-46ad-B32D-E316F5E069BA` (v1). The connector tries the records the headphones
advertise through SDP first.

## Frames

```
3e | type | seq | len (u32 BE) | payload | checksum | 3c
```

* `checksum` is the byte sum of `type..payload`.
* `3c`, `3d` and `3e` inside the frame are sent as `3d` followed by the byte `& ef`.
* Types: `01` ACK, `0c` data table 1, `0e` data table 2.
* Every data frame is acknowledged with an ACK whose sequence is `1 - seq`.
* The app sends one request at a time and waits for its ACK; the ACK's sequence becomes the
  next request's sequence. Unacknowledged requests are retried (see `MdrSession`).

## Startup

1. `00 00` → `01 00 <version u32> <t1> <t2>` (8 bytes on v2, 4 bytes on v1).
2. Device info `04 01` model, `04 02` firmware, `04 03` series/colour.
3. `06 00` (both tables) → list of `(function, priority)` pairs. Everything else is only
   queried when the matching function is advertised (`InitPlan`).

## Commands used

| Feature | Get | Set / reply |
|---|---|---|
| Codec | `12 02` | `13 02 <codec>` |
| Battery L/R | `22 09` | `23 09 <L> <chg> <R> <chg> <thrL> <thrR>` |
| Battery case | `22 0a` | `23 0a <lvl> <chg> <thr>` |
| Auto power off | `26 05` | `28 05 <value> <last>` |
| Power off | | `24 03 01` |
| Equalizer | `56 00` | `58 00 <preset> <n> <steps…>` (Clear Bass + 5 bands, centred on 10) |
| Noise control | `66 17` | `68 17 <settled> <on> <nc/asm> <voice> <level>` |
| Volume | `a6 20` | `a8 20 <0..30>` |
| Playback | `a2 01` | `a4 01 00 <cmd>` |
| General settings | `d0 <id> 01`, `d6 <id>` | `d8 <id> 00 <on>` (names arrive as ids such as `MULTIPOINT_SETTING`) |
| DSEE | `e6 01` | `e8 01 <00/01>` |
| Touch presets | `f6 03` | `f8 03 <n> <left> <right>` |
| Touch capability | `f0 03` | per key: preset list with each gesture's function |
| Ambient cycle | `fa 03` | `fc 03 <n> [<preset> <n> <action> <function>]…` |
| Pause when removed | `f6 01` | `f8 01 <on>` |
| Speak-to-Chat | `f6 0c`, `fa 0c` | `f8 0c <on> 01`, `fc 0c <sensitivity> <timeout>` |
| Head gestures | `f6 0f` | `f8 0f <on>` |
| Voice guidance (table 2) | `46 01`, `46 20` | `48 01 <on>`, `48 20 <vol> 00` |

On/off bytes are inverted on v2: `00` means enabled. Every SET is answered with a NTFY carrying
the applied value, and the earbuds send the same NTFY frames when you use the touch sensors.

### Touch presets

`00` ambient sound control · `35` ambient sound control + Quick Access · `20` playback ·
`10` volume · `ff` not assigned.

Gestures: `00` tap · `01` double · `02` triple · `10` tap and hold. Functions: `01..04` noise
cycles, `10` Quick Attention, `20` play/pause, `21` next, `22` previous, `23`/`24` volume,
`30` voice assistant, `43`/`44` Quick Access.
