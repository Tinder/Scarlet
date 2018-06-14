/*
 * © 2013 - 2018 Tinder, Inc., ALL RIGHTS RESERVED
 */

package com.tinder.scarlet

import io.reactivex.disposables.Disposable

internal data class Session(val webSocket: WebSocket, val webSocketDisposable: Disposable)
