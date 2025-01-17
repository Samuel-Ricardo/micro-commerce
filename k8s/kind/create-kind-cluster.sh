echo "===Starting kind cluster=== 🚀"

kind create cluster --name java-shop --config kind-config.yaml

#echo "Loading Docker Images into Kind Cluster... ⏳"

#chmod +x ./kind-load.sh
#./kind-load.sh

echo "===Kind Cluster Started=== 🎉"

echo ""

echo "===Starting Contaiers=== 🚀"

chmod +x ./apply_all.sh
./apply_all.sh

echo "===Contaiers Started=== 🎉"
