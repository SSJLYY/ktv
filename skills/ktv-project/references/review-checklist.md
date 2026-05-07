# Review Checklist

Use this checklist during repeated codereview passes.

## Backend correctness

- null handling around totals, prices, durations, counts
- status transition safety for room, order, and queue items
- duplicate-submit or repeated state-write windows
- mapper query filters consistent with service assumptions
- timestamps and audit fields preserved
- business exceptions carry useful messages

## Frontend contract

- request wrapper preserves backend payloads on business failures
- page fields match DTO or VO names
- table row keys and ids are stable
- optimistic UI state does not drift from backend response
- polling or refresh logic does not overwrite newer state

## Media and playback

- current play item and queue list stay consistent
- cut-song, replay, top-song, delete-song states are mutually coherent
- media URL, content type, and path resolution align
- room status changes do not leave stale playback state

## Data and configuration

- schema defaults match code assumptions
- seed data does not violate current enum or status assumptions
- Redis key usage matches invalidation logic
- application config defaults are consistent with local behavior

## Documentation quality

- no mojibake copied into new docs
- no stale version claims without code confirmation
- commands point to real module directories
