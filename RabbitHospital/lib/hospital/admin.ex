defmodule Hospital.Admin do
  use GenServer

  ## Client API

  def start() do
    GenServer.start(__MODULE__, [
      {:spec1, "knee"},
      {:spec2, "hip"},
      {:spec3, "elbow"},
      {:spec4, "logs"}
    ])
  end

  def announce(ref, msg) do
    GenServer.cast(ref, {:global, msg})
  end

  def stop(ref) do
    GenServer.stop(ref)
  end

  ## Server callbacks
  def init(opts) do
    {:ok, connection} = AMQP.Connection.open()
    {:ok, channel} = AMQP.Channel.open(connection)

    Enum.map(opts, fn {_, q_name} -> AMQP.Queue.declare(channel, q_name) end)

    {:ok, tag} = AMQP.Basic.consume(channel, opts[:spec4])

    AMQP.Exchange.declare(channel, "with_logs", :topic)
    AMQP.Exchange.declare(channel, "admin_says", :fanout)

    for {_, q_name} <- opts do
      AMQP.Queue.bind(channel, q_name, "with_logs", routing_key: "#{q_name}.#")
      AMQP.Queue.bind(channel, opts[:spec4], "with_logs", routing_key: "#.log")
    end

    {:ok, %{connection: connection, channel: channel, tag: [tag], opts: opts}}
  end

  def handle_info({:basic_consume_ok, _msg}, state) do
    {:noreply, state}
  end

  def handle_info({:basic_deliver, message, meta}, state) do
    IO.puts("[LOG] #{message}")
    AMQP.Basic.ack(state.channel, meta.delivery_tag)
    {:noreply, state}
  end

  def handle_info({:basic_deliver_ok, _message, _meta}, state) do
    {:noreply, state}
  end

  def handle_cast({:global, msg}, state) do
    AMQP.Basic.publish(state.channel, "admin_says", "admin", msg)
    {:noreply, state}
  end

  def terminate(_reason, state) do
    Enum.map(state.tag, fn x -> Hospital.Utils.cancel(state.channel, x) end)
    Enum.map(state.opts, fn {_, q_name} -> AMQP.Queue.delete(state.channel, q_name) end)
    AMQP.Channel.close(state.channel)
    AMQP.Connection.close(state.connection)
  end
end
