/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet

import io.reactivex.disposables.Disposable

internal data class Session(val webSocket: WebSocket, val webSocketDisposable: Disposable)
