package me.kavishdevar.librepods.services

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import me.kavishdevar.librepods.utils.MediaController

fun AirPodsService.connectAudio(
    context: Context,
    device: BluetoothDevice?
) {

    val bluetoothAdapter =
        context.getSystemService(BluetoothManager::class.java).adapter

    bluetoothAdapter?.getProfileProxy(
        context,
        object : BluetoothProfile.ServiceListener {

            override fun onServiceConnected(
                profile: Int,
                proxy: BluetoothProfile
            ) {
                if (profile == BluetoothProfile.A2DP) {
                    try {
                        val policyMethod =
                            proxy.javaClass.getMethod(
                                "setConnectionPolicy",
                                BluetoothDevice::class.java,
                                Int::class.java
                            )

                        policyMethod.invoke(proxy, device, 100)

                        val connectMethod =
                            proxy.javaClass.getMethod(
                                "connect",
                                BluetoothDevice::class.java
                            )

                        connectMethod.invoke(proxy, device)

                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        bluetoothAdapter.closeProfileProxy(
                            BluetoothProfile.A2DP,
                            proxy
                        )

                        if (MediaController.pausedWhileTakingOver) {
                            MediaController.sendPlay()
                        }
                    }
                }
            }

            override fun onServiceDisconnected(profile: Int) {}
        },
        BluetoothProfile.A2DP
    )

    bluetoothAdapter?.getProfileProxy(
        context,
        object : BluetoothProfile.ServiceListener {

            override fun onServiceConnected(
                profile: Int,
                proxy: BluetoothProfile
            ) {
                if (profile == BluetoothProfile.HEADSET) {
                    try {
                        val policyMethod =
                            proxy.javaClass.getMethod(
                                "setConnectionPolicy",
                                BluetoothDevice::class.java,
                                Int::class.java
                            )

                        policyMethod.invoke(proxy, device, 100)

                        val connectMethod =
                            proxy.javaClass.getMethod(
                                "connect",
                                BluetoothDevice::class.java
                            )

                        connectMethod.invoke(proxy, device)

                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        bluetoothAdapter.closeProfileProxy(
                            BluetoothProfile.HEADSET,
                            proxy
                        )
                    }
                }
            }

            override fun onServiceDisconnected(profile: Int) {}
        },
        BluetoothProfile.HEADSET
    )
}