// SPDX-License-Identifier: MIT
pragma solidity ^0.8.17;

contract NewFreezeV2 {
    uint a;

    event BalanceFreezedV2(uint, uint);
    event BalanceUnfreezedV2(uint, uint);
    event AllUnFreezeV2Canceled();
    event ExpireUnfreezeWithdrew(uint);
    event ResourceDelegated(uint, uint, address);
    event ResourceUnDelegated(uint, uint, address);

    constructor() payable {
    }

//    constructor(uint amount, uint res) payable {
//        freezebalancev2(amount, res);
//    }

    receive() payable external {}

    function freeze(address payable receiver, uint amount, uint res) external payable{
        receiver.freeze(amount, res);
    }

    function freezeBalanceV2(uint amount, uint resourceType) external {
        freezebalancev2(amount, resourceType);
        emit BalanceFreezedV2(amount, resourceType);
    }

    function unfreezeBalanceV2(uint amount, uint resourceType) external {
        unfreezebalancev2(amount, resourceType);
        emit BalanceUnfreezedV2(amount, resourceType);
    }

    function cancelAllUnfreezeV2() external {
        cancelallunfreezev2();
        emit AllUnFreezeV2Canceled();
    }

    function withdrawExpireUnfreeze() external returns(uint amount) {
        amount = withdrawexpireunfreeze();
        emit ExpireUnfreezeWithdrew(amount);
    }

    function delegateResource(uint amount, uint resourceType, address payable receiver) external {
        receiver.delegateResource(amount, resourceType);
        emit ResourceDelegated(amount, resourceType, receiver);
    }

    function unDelegateResource(uint amount, uint resourceType, address payable receiver) external {
        receiver.unDelegateResource(amount, resourceType);
        emit ResourceUnDelegated(amount, resourceType, receiver);
    }

    function unDelegateResource1(uint amount, uint resourceType, address payable receiver) external {
        receiver.unDelegateResource(amount, resourceType);
        for (uint i = 0; i < 100; i++) {
            a = i;
        }
        emit ResourceUnDelegated(amount, resourceType, receiver);
    }

    function unDelegateResource2(uint amount, uint resourceType, address payable receiver) external {
        for (uint i = 0; i < 100; i++) {
            a = i;
        }
        receiver.unDelegateResource(amount, resourceType);
        emit ResourceUnDelegated(amount, resourceType, receiver);
    }

    function getChainParameters() view public returns(uint, uint, uint, uint, uint) {
        return (chain.totalNetLimit, chain.totalNetWeight,
        chain.totalEnergyCurrentLimit, chain.totalEnergyWeight,
        chain.unfreezeDelayDays);
    }

    function getAvailableUnfreezeV2Size(address target) view public returns(uint) {
        return target.availableUnfreezeV2Size();
    }

    function getUnfreezableBalanceV2(address target, uint resourceType) view public returns(uint) {
        return target.unfreezableBalanceV2(resourceType);
    }

    function getExpireUnfreezeBalanceV2(address target, uint timestamp) view public returns(uint) {
        return target.expireUnfreezeBalanceV2(timestamp);
    }

    function getDelegatableResource(address target, uint resourceType) view public returns(uint, uint) {
        return (target.delegatableResource(resourceType), block.number);
    }

    function getResourceV2(address target, address from, uint resourceType) view public returns(uint) {
        return target.resourceV2(from, resourceType);
    }

    function checkUnDelegateResource(address target, uint amount, uint resourceType) view public returns(uint, uint, uint, uint) {
        (uint clean, uint dirty, uint restoreTime) = target.checkUnDelegateResource(amount, resourceType);
        return (clean, dirty, restoreTime, block.number);
    }

    function getResourceUsage(address target, uint resourceType) view public returns(uint, uint, uint) {
        (uint dirty, uint restoreTime) = target.resourceUsage(resourceType);
        return (dirty, restoreTime, block.number);
    }

    function getTotalResource(address target, uint resourceType) view public returns(uint) {
        return target.totalResource(resourceType);
    }

    function getTotalDelegatedResource(address from, uint resourceType) view public returns(uint) {
        return from.totalDelegatedResource(resourceType);
    }

    function getTotalAcquiredResource(address target, uint resourceType) view public returns(uint) {
        return target.totalAcquiredResource(resourceType);
    }

    function killme(address payable target) external {
        selfdestruct(target);
    }

    function killme1(address payable target,uint256 amount, uint256 resourceType) external {
        unfreezebalancev2(amount, resourceType);
        cancelallunfreezev2();
        selfdestruct(target);
    }

    function deploy(uint256 salt) public returns(address){
        address addr;
        bytes memory code = type(C).creationCode;
        assembly {
            addr := create2(0, add(code, 0x20), mload(code), salt)
        //if iszero(extcodesize(addr)) {
        //    revert(0, 0)
        //}
        }
        //emit Deployed(addr, salt, msg.sender);
        return addr;
    }

    function voteWitness(address[] calldata srList, uint[] calldata tpList) external {
        vote(srList, tpList);
    }

    function withdrawReward() external returns(uint) {
        return withdrawreward();
    }


    function testConsumeEnergy(int x, int y) external returns (int){
        return x + y;
    }

}

contract C {
    constructor() public payable {}

    function destroy(address payable inheritor) external {
        selfdestruct(inheritor);
    }
}


contract D {
    constructor() public payable {}
    function getPredictedAddress(bytes32 salt) view public returns(address) {
        // This complicated expression just tells you how the address
        // can be pre-computed. It is just there for illustration.
        // You actually only need ``new D{salt: salt}(arg)``.
        address predictedAddress = address(uint160(uint(keccak256(abi.encodePacked(
                bytes1(0x41),
                address(this),
                salt,
                keccak256(abi.encodePacked(
                    type(NewFreezeV2).creationCode
                ))
            )))));
        return predictedAddress;
    }

    address[] public srList;
    uint256[] public tpList;

    function initArray() public {
        srList.push(0x0Be88A918D74D0DfD71Dc84bD4abf036d0562991);
        srList.push(0xBAFB56091591790e00aA05eADdCC7DC1474B5D4B);

        tpList.push(100);
        tpList.push(100);
    }

    function complexCreateKill(bytes32 salt, address payable target, address payable delegate_receiver) public {
        NewFreezeV2 newFreeze = new NewFreezeV2{salt: salt}();

        newFreeze.freezeBalanceV2(100000000, 0);
        newFreeze.freezeBalanceV2(100000000, 1);

        newFreeze.delegateResource(50000000, 0, delegate_receiver);
        newFreeze.delegateResource(50000000, 1, delegate_receiver);

        newFreeze.unDelegateResource(50000000, 0, delegate_receiver);
        newFreeze.unDelegateResource(50000000, 1, delegate_receiver);

        newFreeze.unfreezeBalanceV2(10000000, 0);
        newFreeze.unfreezeBalanceV2(10000000, 1);

        newFreeze.cancelAllUnfreezeV2();
        newFreeze.voteWitness(srList, tpList);
        newFreeze.killme(target);
    }

    function complexCreate(bytes32 salt, address payable target, address payable delegate_receiver) public {
        NewFreezeV2 newFreeze = new NewFreezeV2{salt: salt}();

        newFreeze.freezeBalanceV2(100000000, 0);
        newFreeze.freezeBalanceV2(100000000, 1);

        newFreeze.delegateResource(50000000, 0, delegate_receiver);
        newFreeze.delegateResource(50000000, 1, delegate_receiver);

        newFreeze.unDelegateResource(50000000, 0, delegate_receiver);
        newFreeze.unDelegateResource(50000000, 1, delegate_receiver);

        newFreeze.unfreezeBalanceV2(10000000, 0);
        newFreeze.unfreezeBalanceV2(10000000, 1);

        newFreeze.cancelAllUnfreezeV2();
        newFreeze.voteWitness(srList, tpList);
    }


}


