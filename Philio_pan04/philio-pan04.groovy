/**
 *  
 *	Philio Pan04 Dual Relay Device Type
 *  
 *	Author: Eric Maycock (erocm123)
 *	email: erocmail@gmail.com
 *	Date: 2015-10-29
 * 
 * 	 
 *	Device Type supports all the feautres of the Pan04 device including both switches, 
 *	current energy consumption in W and cumulative energy consumption in kWh.
 */
 
metadata {
definition (name: "Philio PAN04 Dual Relay", namespace: "erocm123", author: "Eric Maycock") {
capability "Switch"
capability "Polling"
capability "Configuration"
capability "Refresh"
capability "Energy Meter"

attribute "switch1", "string"
attribute "switch2", "string"


command "on1"
command "off1"
command "on2"
command "off2"
command "reset"

fingerprint deviceId: "0x1001", inClusters:"0x5E, 0x86, 0x72, 0x5A, 0x85, 0x59, 0x73, 0x25, 0x20, 0x27, 0x71, 0x2B, 0x2C, 0x75, 0x7A, 0x60, 0x32, 0x70"
}

simulator {
status "on": "command: 2003, payload: FF"
status "off": "command: 2003, payload: 00"

// reply messages
reply "2001FF,delay 100,2502": "command: 2503, payload: FF"
reply "200100,delay 100,2502": "command: 2503, payload: 00"
}

tiles {

	standardTile("switch1", "device.switch1",canChangeIcon: true) {
		state "on", label: "switch1", action: "off1", icon: "st.switches.switch.on", backgroundColor: "#79b821"
		state "off", label: "switch1", action: "on1", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
    }
	standardTile("switch2", "device.switch2",canChangeIcon: true) {
		state "on", label: "switch2", action: "off2", icon: "st.switches.switch.on", backgroundColor: "#79b821"
		state "off", label: "switch2", action: "on2", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
    }
    standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
		state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
    }

    standardTile("configure", "device.switch", inactiveLabel: false, decoration: "flat") {
		state "default", label:"", action:"configure", icon:"st.secondary.configure"
    }
    standardTile("reset", "device.energy", inactiveLabel: false, decoration: "flat") {
		state "default", label:'reset kWh', action:"reset"
	}
    valueTile("energy", "device.energy", decoration: "flat") {
			state "default", label:'${currentValue} kWh'
	}
    valueTile("power", "device.power", decoration: "flat") {
			state "default", label:'${currentValue} W'
	}

    main(["switch1", "switch2"])
    details(["switch1","switch2","refresh","energy","power","reset","configure"])
}
}

def parse(String description) {
    def result = []
    def cmd = zwave.parse(description)
    if (cmd) {
        result += zwaveEvent(cmd)
        log.debug "Parsed ${cmd} to ${result.inspect()}"
    } else {
        log.debug "Non-parsed event: ${description}"
    }
    return result
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd)
{
    def result
    if (cmd.value == 0) {
        result = createEvent(name: "switch", value: "off")
    } else {
        result = createEvent(name: "switch", value: "on")
    }
    return result
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd)
{
    sendEvent(name: "switch", value: cmd.value ? "on" : "off", type: "digital")
    def result = []
    result << zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:1, commandClass:37, command:2).format()
    result << zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:2, commandClass:37, command:2).format()
    //result << zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:3, commandClass:37, command:2).format()
    response(delayBetween(result, 1000)) // returns the result of reponse()
}

def zwaveEvent(physicalgraph.zwave.commands.meterv3.MeterReport cmd) {
    def result
    if (cmd.scale == 0) {
        result = createEvent(name: "energy", value: cmd.scaledMeterValue, unit: "kWh")
    } else if (cmd.scale == 1) {
        result = createEvent(name: "energy", value: cmd.scaledMeterValue, unit: "kVAh")
    } else {
        result = createEvent(name: "power", value: cmd.scaledMeterValue, unit: "W")
    }
    return result
}

def zwaveEvent(physicalgraph.zwave.commands.multichannelv3.MultiChannelCapabilityReport cmd) 
{
    log.debug "multichannelv3.MultiChannelCapabilityReport $cmd"
    if (cmd.endPoint == 2 ) {
        def currstate = device.currentState("switch2").getValue()
        if (currstate == "on")
        	sendEvent(name: "switch2", value: "off", isStateChange: true, display: false)
        else if (currstate == "off")
        	sendEvent(name: "switch2", value: "on", isStateChange: true, display: false)
    }
    else if (cmd.endPoint == 1 ) {
        def currstate = device.currentState("switch1").getValue()
        if (currstate == "on")
        sendEvent(name: "switch1", value: "off", isStateChange: true, display: false)
        else if (currstate == "off")
        sendEvent(name: "switch1", value: "on", isStateChange: true, display: false)
    }
}

def zwaveEvent(physicalgraph.zwave.commands.multichannelv3.MultiChannelCmdEncap cmd) {
	def map = [ name: "switch$cmd.sourceEndPoint" ]

	if (cmd.commandClass == 37){
        if (cmd.parameter == [0]) {
            map.value = "off"
        }
        if (cmd.parameter == [255]) {
            map.value = "on"
        }
        createEvent(map)
    }
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
    // This will capture any commands not handled by other instances of zwaveEvent
    // and is recommended for development so you can see every command the device sends
    return createEvent(descriptionText: "${device.displayName}: ${cmd}")
}

def refresh() {
	def cmds = []
	cmds << zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:1, commandClass:37, command:2).format()
    cmds << zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:2, commandClass:37, command:2).format()
    //cmds << zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:3, commandClass:37, command:2).format()
	delayBetween(cmds, 1000)
}

def poll() {
	delayBetween([
	zwave.switchBinaryV1.switchBinaryGet().format(),
	zwave.manufacturerSpecificV1.manufacturerSpecificGet().format()
	], 1000)
}

def reset() {
    delayBetween([
        zwave.meterV2.meterReset().format(),
        zwave.meterV2.meterGet().format()
    ], 1000)
}

def configure() {
	// Commenting this out in case someone tries to use this on the Enerwave Dual Relay. The Philio should come with config parameter
    // 3 set to 3 by default so it should be unecessary. Changing parameter 3 to 3 on the Enerwave will make it send its status
    // reports to node 3 in the network. The Enerwave should have parameter 3 set to 1 though to have it send status updates
    // to the SmartThings hub. By default this parameter is set to 0.
    //delayBetween([
    	//zwave.configurationV1.configurationSet(parameterNumber:3, configurationValue: [3]).format()	// Set switch to report values for both Relay1 and Relay2
    //])
}

def on1() {
    delayBetween([
        zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:1, commandClass:37, command:1, parameter:[255]).format(),
        zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:1, commandClass:37, command:2).format()
    ], 1000)
}

def off1() {
    delayBetween([
        zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:1, commandClass:37, command:1, parameter:[0]).format(),
        zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:1, commandClass:37, command:2).format()
    ], 1000)
}

def on2() {
    delayBetween([
        zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:2, destinationEndPoint:2, commandClass:37, command:1, parameter:[255]).format(),
        zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:2, destinationEndPoint:2, commandClass:37, command:2).format()
    ], 1000)
}

def off2() {
    delayBetween([
        zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:2, destinationEndPoint:2, commandClass:37, command:1, parameter:[0]).format(),
        zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:2, destinationEndPoint:2, commandClass:37, command:2).format()
    ], 1000)
}
