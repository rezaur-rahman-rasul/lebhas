# Maintenance Mode Guide

Maintenance mode is controlled by `MAINTENANCE_MODE`.

When enabled, non-critical write requests are blocked outside master operations. Health, system status, and master maintenance APIs remain available.

Enable only with a clear reason and disable immediately after the operational window is complete.
